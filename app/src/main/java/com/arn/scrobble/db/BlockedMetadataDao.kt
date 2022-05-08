package com.arn.scrobble.db

import androidx.lifecycle.LiveData
import androidx.room.*
import de.umass.lastfm.scrobble.ScrobbleData

@Dao
interface BlockedMetadataDao {
    @get:Query("SELECT * FROM $tableName ORDER BY _id DESC")
    val all: List<BlockedMetadata>

    @get:Query("SELECT * FROM $tableName ORDER BY _id DESC")
    val allLd: LiveData<List<BlockedMetadata>>

    @get:Query("SELECT count(1) FROM $tableName")
    val count: Int

    @Query(
        """SELECT * FROM $tableName
          WHERE (artist IN ("", :artist) OR
          artist IN ("", :ignoredArtist)) AND
          album IN ("", :album) AND
          albumArtist IN ("", :albumArtist) AND
          track IN ("", :track) AND
          NOT (artist == "" AND album == "" AND albumArtist == "" AND track == "")
    """
    )
    fun getBlockedEntries(
        artist: String,
        album: String,
        albumArtist: String,
        track: String,
        ignoredArtist: String?
    ): List<BlockedMetadata>

    fun getBlockedEntry(scrobbleData: ScrobbleData, ignoredArtist: String?): BlockedMetadata? {
        val entries = getBlockedEntries(
            scrobbleData.artist?.lowercase() ?: "",
            scrobbleData.album?.lowercase() ?: "",
            scrobbleData.albumArtist?.lowercase() ?: "",
            scrobbleData.track?.lowercase() ?: "",
            ignoredArtist?.lowercase()
        )

        if (entries.isEmpty()) return null
        if (entries.size == 1) return entries[0]

        // conflict resolution for multiple entries
        val entriesToBlanks = mutableListOf<Pair<BlockedMetadata, Int>>()
        entries.forEach {
            val blanks =
                arrayOf(it.artist, it.album, it.albumArtist, it.track).count { it.isBlank() }
            entriesToBlanks += it to blanks
        }

        entriesToBlanks.sortBy { it.second }

        val lowestBlanks = entriesToBlanks[0].second
        val maxIdx = entriesToBlanks.indexOfLast { it.second == lowestBlanks }
        val bestEntries = entriesToBlanks.map { it.first }.subList(0, maxIdx + 1)
        val bestEntry = bestEntries.first()
        bestEntry.skip = bestEntries.any { it.skip }
        if (!bestEntry.skip)
            bestEntry.mute = bestEntries.any { it.mute }
        return bestEntry
    }


    fun insertLowerCase(entries: List<BlockedMetadata>, ignore: Boolean = false) {
        entries.forEach { entry ->
            entry.track = entry.track.lowercase()
            entry.album = entry.album.lowercase()
            entry.artist = entry.artist.lowercase()
            entry.albumArtist = entry.albumArtist.lowercase()
        }
        if (ignore)
            insertIgnore(entries)
        else
            insert(entries)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(e: List<BlockedMetadata>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(e: List<BlockedMetadata>)

    @Delete
    fun delete(e: BlockedMetadata)

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {
        const val tableName = "blockedMetadata"
    }
}