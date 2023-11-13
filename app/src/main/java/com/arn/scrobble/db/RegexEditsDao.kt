package com.arn.scrobble.db

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arn.scrobble.App
import com.arn.scrobble.NLService
import com.arn.scrobble.Stuff
import com.arn.scrobble.edits.RegexPresets
import de.umass.lastfm.scrobble.ScrobbleData

@Dao
interface RegexEditsDao {
    @Query("SELECT * FROM $tableName ORDER BY `order` ASC LIMIT ${Stuff.MAX_PATTERNS}")
    fun all(): List<RegexEdit>

    @Query("SELECT * FROM $tableName ORDER BY `order` ASC LIMIT ${Stuff.MAX_PATTERNS}")
    fun allLd(): LiveData<List<RegexEdit>>

    @Query("SELECT count(1) FROM $tableName")
    fun count(): Int

    @Query("SELECT count(1) FROM $tableName")
    fun countLd(): LiveData<Int>

    @Query("SELECT MAX(`order`) FROM $tableName")
    fun maxOrder(): Int?

    @Query("SELECT * FROM $tableName WHERE preset IS NOT NULL ORDER BY `order` ASC LIMIT ${Stuff.MAX_PATTERNS}")
    fun allPresets(): List<RegexEdit>

    @Query("SELECT count(1) FROM $tableName WHERE packages IS NOT NULL")
    fun hasPkgNameLd(): LiveData<Boolean>

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

        fun RegexEditsDao.performRegexReplace(
            scrobbleData: ScrobbleData,
            pkgName: String? = null, // null means all
            regexEdits: List<RegexEdit> = all().map { RegexPresets.getPossiblePreset(it) },
        ): Map<String, Set<RegexEdit>> {
            val numMatches = mutableMapOf(
                NLService.B_ARTIST to mutableSetOf<RegexEdit>(),
                NLService.B_ALBUM to mutableSetOf(),
                NLService.B_ALBUM_ARTIST to mutableSetOf(),
                NLService.B_TRACK to mutableSetOf(),
            )

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

                        text = if (regexEdit.replaceAll)
                            text.replace(regex, regexEdit.replacement).trim()
                        else
                            text.replaceFirst(regex, regexEdit.replacement).trim()
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

                        val (track, album, artist, albumArtist) = arrayOf(
                            NLService.B_TRACK,
                            NLService.B_ALBUM,
                            NLService.B_ARTIST,
                            "albumArtist"
                        ).map { groupName ->
                            scrobbleDataToRegexes.forEach { (sdField, regex) ->
                                if (regex.pattern.isEmpty()) return@forEach
                                val namedGroups =
                                    regex.find(sdField)?.groups ?: return@forEach
                                val groupValue =
                                    runCatching { namedGroups[groupName] }.getOrNull()
                                        ?: return@forEach

                                numMatches[groupName.lowercase()]?.add(regexEdit)
                                return@map groupValue.value
                            }
                            null
                        }

                        val found =
                            track != null || album != null || artist != null || albumArtist != null

                        if (track != null) scrobbleData.track = track
                        if (album != null) scrobbleData.album = album
                        if (artist != null) scrobbleData.artist = artist
                        if (albumArtist != null) scrobbleData.albumArtist = albumArtist

                        if (found && !regexEdit.continueMatching)
                            return
                    }
            }


            try {
                scrobbleData.artist = replaceField(scrobbleData.artist, NLService.B_ARTIST)
                scrobbleData.album = replaceField(scrobbleData.album, NLService.B_ALBUM)
                scrobbleData.albumArtist =
                    replaceField(scrobbleData.albumArtist, NLService.B_ALBUM_ARTIST)
                scrobbleData.track = replaceField(scrobbleData.track, NLService.B_TRACK)

                // needs java 8
                if (App.prefs.proStatus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    extract()
            } catch (e: IllegalArgumentException) {
                Stuff.logW("regex error: ${e.message}")
            }

            return numMatches
        }

    }
}