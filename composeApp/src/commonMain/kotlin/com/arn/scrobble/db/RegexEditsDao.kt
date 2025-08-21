package com.arn.scrobble.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import co.touchlab.kermit.Logger
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.Flow

@Dao
interface RegexEditsDao {
    @Query("SELECT * FROM $tableName ORDER BY `order` ASC")
    suspend fun allWithoutLimit(): List<RegexEdit>

    @Query("SELECT * FROM $tableName ORDER BY `order` ASC LIMIT ${Stuff.MAX_PATTERNS}")
    fun allFlow(): Flow<List<RegexEdit>>

    @Query("SELECT count(1) FROM $tableName")
    fun count(): Flow<Int>

    @Query("SELECT MAX(`order`) FROM $tableName")
    suspend fun maxOrder(): Int?

    @Query("UPDATE $tableName SET `order` = `order` + 1")
    suspend fun shiftDown()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: List<RegexEdit>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(e: List<RegexEdit>)

    @Delete
    suspend fun delete(e: RegexEdit)

    @Query("DELETE FROM $tableName")
    suspend fun nuke()

    companion object {
        const val tableName = "regexEdits"

        suspend fun RegexEditsDao.import(e: List<RegexEdit>) {
            val existingWithoutOrder =
                allWithoutLimit().map { it.copy(order = -1, _id = -1) }.toSet()
            val toInsert = e.filter {
                it.copy(order = -1, _id = -1) !in existingWithoutOrder
            }

            if (toInsert.isNotEmpty())
                insertIgnore(toInsert)
        }

        fun RegexEdit.countNamedCaptureGroups(): Map<RegexEdit.Field, Int> {

            return RegexEdit.Field.entries.associateWith { field ->
                arrayOf(
                    search.searchTrack,
                    search.searchAlbum,
                    search.searchArtist,
                    search.searchAlbumArtist,
                )
                    .filterNot { it.isEmpty() }
                    .sumOf { it.split("(?<$field>").size - 1 }
            }
        }

        fun performRegexReplace(
            scrobbleData: ScrobbleData,
            regexEdits: List<RegexEdit>
        ): RegexResults {
            val isLicenseValid = PlatformStuff.billingRepository.isLicenseValid
            var scrobbleData = scrobbleData
            val cumulativeMatches = mutableSetOf<RegexEdit>()

            regexEdits
                .filter {
                    it.appIds.isEmpty() || scrobbleData.appId in it.appIds
                }.forEach { regexEdit ->
                    val scrobbleDataToMatches = listOf(
                        scrobbleData.track to regexEdit.search.searchTrack,
                        scrobbleData.album.orEmpty() to regexEdit.search.searchAlbum,
                        scrobbleData.artist to regexEdit.search.searchArtist,
                        scrobbleData.albumArtist.orEmpty() to regexEdit.search.searchAlbumArtist
                    )
                        .filterNot { (_, regexStr) ->
                            regexStr.isEmpty()
                        }
                        .map { (fieldData, regexStr) ->
                            val regexOptions = mutableSetOf<RegexOption>()
                            if (!regexEdit.caseSensitive)
                                regexOptions += RegexOption.IGNORE_CASE

                            val regex = runCatching { regexStr.toRegex(regexOptions) }
                                .onFailure {
                                    Logger.e(it) { "Failed to compile regex for field ${regexEdit.search}" }
                                }
                                .getOrNull()

                            fieldData to regex?.find(fieldData)
                        }

                    val allFieldDataMatched = scrobbleDataToMatches.all { (_, match) ->
                        match != null
                    }

                    if (allFieldDataMatched) {
                        when (regexEdit.mode()) {
                            RegexMode.Block -> {
                                if (isLicenseValid)
                                    return RegexResults(
                                        matches = setOf(regexEdit),
                                        scrobbleData = null,
                                        blockPlayerAction = regexEdit.blockPlayerAction,
                                    )
                            }

                            RegexMode.Extract -> {
                                if (isLicenseValid && PlatformStuff.isJava8OrGreater) {
                                    val newScrobbleData = extract(
                                        scrobbleData,
                                        scrobbleDataToMatches
                                    )
                                    if (newScrobbleData != null) {
                                        cumulativeMatches += regexEdit
                                        scrobbleData = newScrobbleData
                                    }
                                }
                            }

                            RegexMode.Replace -> {
                                val newScrobbleData =
                                    runCatching { replace(scrobbleData, regexEdit) }
                                        .onFailure {
                                            Logger.e(it) { "Failed to compile regex for field ${regexEdit.search}" }
                                        }
                                        .getOrNull()

                                if (newScrobbleData != null) {
                                    cumulativeMatches += regexEdit
                                    scrobbleData = newScrobbleData
                                }
                            }
                        }
                    }
                }

            return RegexResults(
                scrobbleData = scrobbleData.takeIf { cumulativeMatches.isNotEmpty() },
                blockPlayerAction = null,
                matches = cumulativeMatches,
            )
        }

        private fun extract(
            scrobbleData: ScrobbleData,
            scrobbleDataToMatches: List<Pair<String, MatchResult?>>
        ): ScrobbleData? {
            var newTrack: String? = null
            var newArtist: String? = null
            var newAlbum: String? = null
            var newAlbumArtist: String? = null

            scrobbleDataToMatches.forEach { (_, match) ->

                if (newTrack == null)
                    newTrack = try {
                        match?.groups?.get(RegexEdit.Field.track.name)?.value
                    } catch (_: IllegalArgumentException) {
                        null
                    }

                if (newArtist == null)
                    newArtist = try {
                        match?.groups?.get(RegexEdit.Field.artist.name)?.value
                    } catch (_: IllegalArgumentException) {
                        null
                    }

                if (newAlbum == null)
                    newAlbum = try {
                        match?.groups?.get(RegexEdit.Field.album.name)?.value
                    } catch (_: IllegalArgumentException) {
                        null
                    }

                if (newAlbumArtist == null)
                    newAlbumArtist = try {
                        match?.groups?.get(RegexEdit.Field.albumArtist.name)?.value
                    } catch (_: IllegalArgumentException) {
                        null
                    }
            }


            if (!newTrack.isNullOrEmpty() && !newArtist.isNullOrEmpty()) {
                return scrobbleData.copy(
                    track = newTrack,
                    artist = newArtist,
                    album = newAlbum,
                    albumArtist = newAlbumArtist,
                )
            }

            return null
        }

        private fun replace(
            scrobbleData: ScrobbleData,
            regexEdit: RegexEdit
        ): ScrobbleData? {
            if (regexEdit.replacement == null) return null

            val regexOptions = mutableSetOf<RegexOption>()
            if (!regexEdit.caseSensitive)
                regexOptions += RegexOption.IGNORE_CASE

            val newTrack: String?
            val newArtist: String?
            val newAlbum: String?
            val newAlbumArtist: String?

            val trackRegex = regexEdit.search.searchTrack.toRegex(regexOptions)
            val albumRegex = regexEdit.search.searchAlbum.toRegex(regexOptions)
            val artistRegex = regexEdit.search.searchArtist.toRegex(regexOptions)
            val albumArtistRegex = regexEdit.search.searchAlbumArtist.toRegex(regexOptions)

            if (!regexEdit.replacement.replaceAll) {
                newTrack = trackRegex.replace(
                    scrobbleData.track,
                    regexEdit.replacement.replacementTrack
                )
                newAlbum = albumRegex.replace(
                    scrobbleData.album.orEmpty(),
                    regexEdit.replacement.replacementAlbum
                )
                newArtist = artistRegex.replace(
                    scrobbleData.artist,
                    regexEdit.replacement.replacementArtist
                )
                newAlbumArtist = albumArtistRegex.replace(
                    scrobbleData.albumArtist.orEmpty(),
                    regexEdit.replacement.replacementAlbumArtist
                )
            } else {
                newTrack = trackRegex.replaceFirst(
                    scrobbleData.track,
                    regexEdit.replacement.replacementTrack
                )
                newAlbum = albumRegex.replaceFirst(
                    scrobbleData.album.orEmpty(),
                    regexEdit.replacement.replacementAlbum
                )
                newArtist = artistRegex.replaceFirst(
                    scrobbleData.artist,
                    regexEdit.replacement.replacementArtist
                )
                newAlbumArtist = albumArtistRegex.replaceFirst(
                    scrobbleData.albumArtist.orEmpty(),
                    regexEdit.replacement.replacementAlbumArtist
                )
            }


            if (newTrack.isNotEmpty() && newArtist.isNotEmpty()) {
                return scrobbleData.copy(
                    track = newTrack,
                    artist = newArtist,
                    album = newAlbum.ifEmpty { null },
                    albumArtist = newAlbumArtist.ifEmpty { null },
                )
            }

            return null
        }
    }
}