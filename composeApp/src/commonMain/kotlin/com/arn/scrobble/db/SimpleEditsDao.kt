package com.arn.scrobble.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arn.scrobble.api.lastfm.ScrobbleData
import kotlinx.coroutines.flow.Flow


/**
 * Created by arn on 11/09/2017.
 */

@Dao
interface SimpleEditsDao {
    @Query("SELECT * FROM $tableName ORDER BY _id DESC")
    fun allFlow(): Flow<List<SimpleEdit>>

    @Query(
        """
    SELECT * FROM $tableName
    WHERE (hasOrigArtist = 0 OR origArtist = :artist)
      AND (hasOrigTrack = 0 OR origTrack = :track)
      AND (hasOrigAlbum = 0 OR origAlbum = :album)
      AND (hasOrigAlbumArtist = 0 OR origAlbumArtist = :albumArtist)
    ORDER BY
      hasOrigArtist + hasOrigTrack + hasOrigAlbum + hasOrigAlbumArtist DESC
    LIMIT 1
  """
    )
    suspend fun find(
        track: String,
        artist: String,
        album: String,
        albumArtist: String,
    ): SimpleEdit?

    @Query(
        """
        SELECT * FROM $tableName
        WHERE
            (origArtist LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%')
            OR (origAlbum LIKE '%' || :term || '%' OR album LIKE '%' || :term || '%')
            OR (origTrack LIKE '%' || :term || '%' OR track LIKE '%' || :term || '%')
            OR (origAlbumArtist LIKE '%' || :term || '%' OR albumArtist LIKE '%' || :term || '%')
        ORDER BY _id DESC
"""
    )
    fun searchPartial(term: String): Flow<List<SimpleEdit>>

    @Query("SELECT count(1) FROM $tableName")
    fun count(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: List<SimpleEdit>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(e: List<SimpleEdit>)

    @Delete
    suspend fun delete(e: SimpleEdit)

    @Query("DELETE FROM $tableName")
    suspend fun nuke()

    companion object {
        const val tableName = "simpleEdits"

        suspend fun SimpleEditsDao.performEdit(
            scrobbleData: ScrobbleData,
        ): ScrobbleData? {

            var track = scrobbleData.track
            var artist = scrobbleData.artist
            var album = scrobbleData.album
            var albumArtist = scrobbleData.albumArtist

            val foundEdit = find(
                track.lowercase(),
                artist.lowercase(),
                album?.lowercase() ?: "",
                albumArtist?.lowercase() ?: "",
            ) ?: return null

            if (!foundEdit.track.isNullOrEmpty())
                track = foundEdit.track

            if (!foundEdit.artist.isNullOrEmpty())
                artist = foundEdit.artist

            if (foundEdit.album != null)
                album = foundEdit.album

            if (foundEdit.albumArtist != null)
                albumArtist = foundEdit.albumArtist

            return scrobbleData.copy(
                track = track,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
            )
        }

        suspend fun SimpleEditsDao.insertReplaceLowerCase(e: SimpleEdit) {
            val edit = e.copy(
                origTrack = e.origTrack.takeIf { e.hasOrigTrack }?.lowercase() ?: "",
                track = e.track?.trim(),

                origArtist = e.origArtist.takeIf { e.hasOrigArtist }?.lowercase() ?: "",
                artist = e.artist?.trim(),

                origAlbum = e.origAlbum.takeIf { e.hasOrigAlbum }?.lowercase() ?: "",
                album = e.album?.trim(),

                origAlbumArtist = e.origAlbumArtist.takeIf { e.hasOrigAlbumArtist }?.lowercase()
                    ?: "",
                albumArtist = e.albumArtist?.trim(),
            )
            insert(listOf(edit))
        }

    }
}
