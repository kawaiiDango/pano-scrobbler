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

        fun SimpleEdit.performEdit(
            scrobbleData: ScrobbleData,
        ): ScrobbleData {
            return scrobbleData.copy(
                track = track.takeIf { !it.isNullOrEmpty() } ?: scrobbleData.track,
                artist = artist.takeIf { !it.isNullOrEmpty() } ?: scrobbleData.artist,
                album = album ?: scrobbleData.album,
                albumArtist = albumArtist ?: scrobbleData.albumArtist,
            )
        }

        suspend fun SimpleEditsDao.findAndPerformEdit(
            scrobbleData: ScrobbleData,
        ): ScrobbleData? {
            return find(
                scrobbleData.track.lowercase(),
                scrobbleData.artist.lowercase(),
                scrobbleData.album?.lowercase() ?: "",
                scrobbleData.albumArtist?.lowercase() ?: "",
            )?.performEdit(scrobbleData)
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
