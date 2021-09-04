package com.arn.scrobble.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.arn.scrobble.NLService
import com.arn.scrobble.Stuff
import com.arn.scrobble.edits.RegexPresets
import de.umass.lastfm.scrobble.ScrobbleData

private const val tableName = "regexEdits"

@Dao
interface RegexEditsDao {
    @get:Query("SELECT * FROM $tableName ORDER BY `order` ASC LIMIT ${Stuff.MAX_PATTERNS}")
    val all: List<RegexEdit>

    @get:Query("SELECT * FROM $tableName ORDER BY `order` ASC LIMIT ${Stuff.MAX_PATTERNS}")
    val allLd: LiveData<List<RegexEdit>>

    @get:Query("SELECT count(1) FROM $tableName")
    val count: Int

    @get:Query("SELECT count(1) FROM $tableName")
    val countLd: LiveData<Int>

    @get:Query("SELECT MAX(`order`) FROM $tableName")
    val maxOrder: Int?

    @get:Query("SELECT * FROM $tableName WHERE preset IS NOT NULL ORDER BY `order` ASC LIMIT ${Stuff.MAX_PATTERNS}")
    val allPresets: List<RegexEdit>

    @get:Query("SELECT * FROM $tableName WHERE pattern IS NOT NULL ORDER BY `order` ASC LIMIT ${Stuff.MAX_PATTERNS}")
    val allRegexes: List<RegexEdit>

    @Query("UPDATE $tableName SET `order` = `order` + 1")
    fun shiftDown()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(e: List<RegexEdit>)

    fun insert(e: RegexEdit) = insert(listOf(e))

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(e: List<RegexEdit>)

    fun insertIgnore(e: RegexEdit) = insertIgnore(listOf(e))

    @Delete
    fun delete(e: RegexEdit)

    @Query("DELETE FROM $tableName")
    fun nuke()

    fun performRegexReplace(
        scrobbleData: ScrobbleData,
        regexEdits: List<RegexEdit> = all.map { RegexPresets.getPossiblePreset(it) },
        matchedRegexEditsRef: MutableList<RegexEdit>? = null,
    ): Map<String, Int> {
        val numMatches = mutableMapOf(
            NLService.B_ARTIST to 0,
            NLService.B_ALBUM to 0,
            NLService.B_ALBUM_ARTIST to 0,
            NLService.B_TRACK to 0,
        )

        fun replaceField(textp: String?, field: String): String? {
            textp ?: return null
            var text: String = textp
            for (regexEdit in regexEdits.filter { it.field == field }) {
                regexEdit.pattern ?: continue

                val regexOptions = mutableSetOf<RegexOption>()
                if (!regexEdit.caseSensitive)
                    regexOptions += RegexOption.IGNORE_CASE

                val regex = regexEdit.pattern!!.toRegex(regexOptions)

                if (regex.containsMatchIn(text)) {
                    numMatches[field] = numMatches[field]!! + 1
                    matchedRegexEditsRef?.add(regexEdit)

                    text = if (regexEdit.replaceAll)
                        text.replace(regex, regexEdit.replacement).trim()
                    else
                        text.replaceFirst(regex, regexEdit.replacement).trim()
                    if (!regexEdit.continueMatching)
                        break
                }
            }
            return text
        }

        scrobbleData.artist = replaceField(scrobbleData.artist, NLService.B_ARTIST)
        scrobbleData.album = replaceField(scrobbleData.album, NLService.B_ALBUM)
        scrobbleData.albumArtist = replaceField(scrobbleData.albumArtist, NLService.B_ALBUM_ARTIST)
        scrobbleData.track = replaceField(scrobbleData.track, NLService.B_TRACK)

        return numMatches
    }
}