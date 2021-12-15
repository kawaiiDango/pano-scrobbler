package com.arn.scrobble.db

import androidx.lifecycle.LiveData
import androidx.room.*
import de.umass.lastfm.scrobble.ScrobbleData

private const val tableName = "blockedMetadata"

@Dao
interface BlockedMetadataDao {
    @get:Query("SELECT * FROM $tableName ORDER BY _id DESC")
    val all: List<BlockedMetadata>

    @get:Query("SELECT * FROM $tableName ORDER BY _id DESC")
    val allLd: LiveData<List<BlockedMetadata>>

    @get:Query("SELECT count(1) FROM $tableName")
    val count: Int

    @Query(
        """SELECT count(1) FROM $tableName
          WHERE (artist IN ("", :artist) OR
          artist IN ("", :ignoredArtist)) AND
          album IN ("", :album) AND
          albumArtist IN ("", :albumArtist) AND
          track IN ("", :track) AND
          NOT (artist == "" AND album == "" AND albumArtist == "" AND track == "")
    """
    )
    fun isBlocked(
        artist: String,
        album: String,
        albumArtist: String,
        track: String,
        ignoredArtist: String?
    ): Boolean

    fun isBlocked(scrobbleData: ScrobbleData, ignoredArtist: String?) = isBlocked(
        scrobbleData.artist?.lowercase() ?: "",
        scrobbleData.album?.lowercase() ?: "",
        scrobbleData.albumArtist?.lowercase() ?: "",
        scrobbleData.track?.lowercase() ?: "",
        ignoredArtist?.lowercase()
    )


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
}