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

    @Query("SELECT * FROM $tableName WHERE (origArtist = :artist and origAlbum = :album and origTrack = :track) OR legacyHash = :hash")
    suspend fun findByNamesOrHash(
        artist: String,
        album: String,
        track: String,
        hash: String,
    ): SimpleEdit?

    @Query(
        """
        SELECT * FROM $tableName
        WHERE
            (origArtist LIKE '%' || :term || '%' OR artist LIKE '%' || :term || '%')
            OR (origAlbum LIKE '%' || :term || '%' OR album LIKE '%' || :term || '%')
            OR (origTrack LIKE '%' || :term || '%' OR track LIKE '%' || :term || '%')
            OR (albumArtist LIKE '%' || :term || '%')
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

    @Query("DELETE FROM $tableName WHERE legacyHash = :hash")
    suspend fun deleteLegacy(hash: String)

    @Query("DELETE FROM $tableName")
    suspend fun nuke()

    companion object {
        const val tableName = "simpleEdits"

        suspend fun SimpleEditsDao.performEdit(
            scrobbleData: ScrobbleData,
            allowBlankAlbumArtist: Boolean = true,
        ): SimpleEdit? {
            val artist = scrobbleData.artist
            val album = scrobbleData.album
            val track = scrobbleData.track

            val hash = if (artist == "" && track != "")
                track.hashCode().toString() + album.hashCode().toString() + artist.hashCode()
                    .toString()
            else
                artist.hashCode().toString() + album.hashCode().toString() + track.hashCode()
                    .toString()
            val edit =
                findByNamesOrHash(
                    artist.lowercase(),
                    album?.lowercase() ?: "",
                    track.lowercase(),
                    hash
                )
            if (edit != null) {
                scrobbleData.artist = edit.artist
                scrobbleData.album = edit.album
                scrobbleData.track = edit.track
                if (edit.albumArtist.isNotBlank() || allowBlankAlbumArtist)
                    scrobbleData.albumArtist = edit.albumArtist
            }
            return edit
        }

        suspend fun SimpleEditsDao.insertReplaceLowerCase(e: SimpleEdit) {
            val edit = e.copy(
                origArtist = e.origArtist.lowercase(),
                origAlbum = e.origAlbum.lowercase(),
                origTrack = e.origTrack.lowercase(),
            )
            insert(listOf(edit))
        }

    }
}
