package com.arn.scrobble.db

import androidx.lifecycle.LiveData
import androidx.room.*
import de.umass.lastfm.scrobble.ScrobbleData


/**
 * Created by arn on 11/09/2017.
 */

@Dao
interface SimpleEditsDao {
    @get:Query("SELECT * FROM $tableName ORDER BY _id DESC")
    val all: List<SimpleEdit>

    @get:Query("SELECT * FROM $tableName ORDER BY _id DESC")
    val allLd: LiveData<List<SimpleEdit>>

    @Query("SELECT * FROM $tableName WHERE (origArtist = :artist and origAlbum = :album and origTrack = :track) OR legacyHash = :hash")
    fun findByNamesOrHash(artist: String, album: String, track: String, hash: String): SimpleEdit?

    fun performEdit(scrobbleData: ScrobbleData, allowBlankAlbumArtist: Boolean = true): SimpleEdit? {
        val artist = scrobbleData.artist
        val album = scrobbleData.album
        val track = scrobbleData.track

        val hash = if (artist == "" && track != "")
            track.hashCode().toString() + album.hashCode().toString() + artist.hashCode().toString()
        else
            artist.hashCode().toString() + album.hashCode().toString() + track.hashCode().toString()
        val edit = findByNamesOrHash(artist.lowercase(), album.lowercase(), track.lowercase(), hash)
        if (edit != null) {
            scrobbleData.artist = edit.artist
            scrobbleData.album = edit.album
            scrobbleData.track = edit.track
            if (edit.albumArtist.isNotBlank() || allowBlankAlbumArtist)
                scrobbleData.albumArtist = edit.albumArtist
        }
        return edit
    }

    @get:Query("SELECT count(1) FROM $tableName")
    val count: Int

    fun insertReplaceLowerCase(e: SimpleEdit) {
        e.origArtist = e.origArtist.lowercase()
        e.origAlbum = e.origAlbum.lowercase()
        e.origTrack = e.origTrack.lowercase()
        insert(e)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(e: List<SimpleEdit>)

    fun insert(e: SimpleEdit) = insert(listOf(e))

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(e: List<SimpleEdit>)

    fun insertIgnore(e: SimpleEdit) = insertIgnore(listOf(e))

    @Delete
    fun delete(e: SimpleEdit)

    @Query("DELETE FROM $tableName WHERE legacyHash = :hash")
    fun deleteLegacy(hash: String)

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {
        const val tableName = "simpleEdits"
    }
}
