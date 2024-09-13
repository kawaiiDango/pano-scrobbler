package com.arn.scrobble.db

import android.os.Build
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arn.scrobble.NLService
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.edits.RegexPresets
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import timber.log.Timber

@Dao
interface RegexEditsDao {
    @Query("SELECT * FROM $tableName ORDER BY `order` ASC")
    fun allWithoutLimit(): List<RegexEdit>

    @Query("SELECT * FROM $tableName ORDER BY `order` ASC LIMIT ${Stuff.MAX_PATTERNS}")
    fun allFlow(): Flow<List<RegexEdit>>

    @Query("SELECT count(1) FROM $tableName")
    fun count(): Flow<Int>

    @Query("SELECT MAX(`order`) FROM $tableName")
    fun maxOrder(): Int?

    @Query("SELECT * FROM $tableName WHERE preset IS NOT NULL ORDER BY `order` ASC LIMIT ${Stuff.MAX_PATTERNS}")
    fun allPresets(): Flow<List<RegexEdit>>

    @Query("SELECT count(1) FROM $tableName WHERE packages IS NOT NULL")
    fun hasPkgNameFlow(): Flow<Boolean>

    @Query("UPDATE $tableName SET `order` = `order` + 1")
    fun shiftDown()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(e: List<RegexEdit>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(e: List<RegexEdit>)

    @Delete
    fun delete(e: RegexEdit)

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {
        const val tableName = "regexEdits"

        fun RegexEditsDao.import(e: List<RegexEdit>) {
            val existingWithoutOrder =
                allWithoutLimit().map { it.copy(order = -1, _id = -1) }.toSet()
            val toInsert = e.filter {
                it.copy(order = -1, _id = -1) !in existingWithoutOrder
            }

            if (toInsert.isNotEmpty())
                insertIgnore(toInsert)
        }

        fun RegexEdit.countNamedCaptureGroups(): Map<String, Int> {
            val extractionPatterns = extractionPatterns ?: return emptyMap()

            return arrayOf(
                NLService.B_TRACK,
                NLService.B_ALBUM,
                NLService.B_ARTIST,
                "albumArtist"
            ).associateWith { groupName ->
                arrayOf(
                    extractionPatterns.extractionTrack,
                    extractionPatterns.extractionAlbum,
                    extractionPatterns.extractionArtist,
                    extractionPatterns.extractionAlbumArtist,
                )
                    .filterNot { it.isEmpty() }
                    .sumOf { it.split("(?<$groupName>").size - 1 }
            }
        }

        suspend fun RegexEditsDao.performRegexReplace(
            scrobbleData: ScrobbleData,
            pkgName: String? = null, // null means all
            regexEditsp: List<RegexEdit>? = null, // null means all
        ): Map<String, Set<RegexEdit>> {
            val numMatches = mutableMapOf(
                NLService.B_ARTIST to mutableSetOf<RegexEdit>(),
                NLService.B_ALBUM to mutableSetOf(),
                NLService.B_ALBUM_ARTIST to mutableSetOf(),
                NLService.B_TRACK to mutableSetOf(),
            )

            val regexEdits =
                regexEditsp ?: allFlow().first().map { RegexPresets.getPossiblePreset(it) }

            fun replaceField(textp: String?, field: String): String? {
                textp ?: return null
                var text: String = textp
                regexEdits.filter {
                    it.pattern != null &&
                            it.fields != null &&
                            field in it.fields!! &&
                            (it.packages.isNullOrEmpty() || pkgName == null || pkgName in it.packages!!)
                }.forEach { regexEdit ->
                    val regexOptions = mutableSetOf<RegexOption>()
                    if (!regexEdit.caseSensitive)
                        regexOptions += RegexOption.IGNORE_CASE

                    val regex = regexEdit.pattern!!.toRegex(regexOptions)

                    if (regex.containsMatchIn(text)) {
                        numMatches[field]?.add(regexEdit)

                        kotlin.runCatching {
                            text = if (regexEdit.replaceAll)
                                text.replace(regex, regexEdit.replacement).trim()
                            else
                                text.replaceFirst(regex, regexEdit.replacement).trim()
                        }

                        if (!regexEdit.continueMatching)
                            return text
                    }
                }
                return text
            }

            fun extract() {
                regexEdits
                    .filter {
                        it.extractionPatterns != null &&
                                (it.packages.isNullOrEmpty() || pkgName == null || pkgName in it.packages!!)
                    }.forEachIndexed { _, regexEdit ->
                        val extractionPatterns = regexEdit.extractionPatterns!!

                        val scrobbleDataToRegexes = mapOf(
                            scrobbleData.track to extractionPatterns.extractionTrack,
                            scrobbleData.album to extractionPatterns.extractionAlbum,
                            scrobbleData.artist to extractionPatterns.extractionArtist,
                            scrobbleData.albumArtist to extractionPatterns.extractionAlbumArtist,
                        ).mapValues { (key, value) ->
                            if (regexEdit.caseSensitive)
                                value.toRegex()
                            else
                                value.toRegex(RegexOption.IGNORE_CASE)
                        }

                        val namedCaptureGroupsCount = regexEdit.countNamedCaptureGroups()

                        val extractionsMap = arrayOf(
                            NLService.B_TRACK,
                            NLService.B_ALBUM,
                            NLService.B_ARTIST,
                            "albumArtist"
                        ).associateWith { groupName ->
                            scrobbleDataToRegexes.forEach { (sdField, regex) ->
                                if (regex.pattern.isEmpty() || sdField.isNullOrEmpty()) return@forEach
                                val namedGroups =
                                    regex.find(sdField)?.groups ?: return@forEach
                                val groupValue =
                                    runCatching { namedGroups[groupName] }.getOrNull()
                                        ?: return@forEach

                                return@associateWith groupValue.value
                            }
                            null
                        }

                        val allFound = extractionsMap.all { (sdField, extraction) ->
                            val count = namedCaptureGroupsCount[sdField] ?: 0
                            count == 1 && extraction != null ||
                                    count == 0 && extraction == null
                        }

                        if (allFound) {
                            extractionsMap.forEach { (sdField, extraction) ->
                                if (extraction != null)
                                    numMatches[sdField.lowercase()]?.add(regexEdit)
                            }

                            scrobbleData.track = extractionsMap[NLService.B_TRACK] ?: ""
                            scrobbleData.album = extractionsMap[NLService.B_ALBUM] ?: ""
                            scrobbleData.artist = extractionsMap[NLService.B_ARTIST] ?: ""
                            scrobbleData.albumArtist = extractionsMap["albumArtist"] ?: ""

                            if (!regexEdit.continueMatching)
                                return
                        }


                    }
            }


            try {
                scrobbleData.artist = replaceField(scrobbleData.artist, NLService.B_ARTIST)!!
                scrobbleData.album = replaceField(scrobbleData.album, NLService.B_ALBUM)
                scrobbleData.albumArtist =
                    replaceField(scrobbleData.albumArtist, NLService.B_ALBUM_ARTIST)
                scrobbleData.track = replaceField(scrobbleData.track, NLService.B_TRACK)!!

                // needs java 8
                if (Stuff.billingRepository.isLicenseValid && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    extract()
            } catch (e: IllegalArgumentException) {
                Timber.w("regex error: ${e.message}")
            }

            return numMatches
        }

    }
}