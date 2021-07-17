package com.arn.scrobble.db

import androidx.lifecycle.LiveData
import androidx.room.*


/**
 * Created by arn on 11/09/2017.
 */
private const val tableName = "simpleEdits"

@Dao
interface SimpleEditsDao {
    @get:Query("SELECT * FROM $tableName ORDER BY _id DESC")
    val all: List<SimpleEdit>

    @get:Query("SELECT * FROM $tableName ORDER BY _id DESC")
    val allLd: LiveData<List<SimpleEdit>>

    @Query("SELECT * FROM $tableName WHERE (origArtist = :artist and origAlbum = :album and origTrack = :track) OR legacyHash = :hash")
    fun findByNamesOrHash(artist: String, album: String, track: String, hash: String): SimpleEdit?

    fun find(artist: String, album: String, track: String): SimpleEdit? {
        val hash = if (artist == "" && track != "")
            track.hashCode().toString() + album.hashCode().toString() + artist.hashCode().toString()
        else
            artist.hashCode().toString() + album.hashCode().toString() + track.hashCode().toString()
        return findByNamesOrHash(artist.lowercase(), album.lowercase(), track.lowercase(), hash)
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
}
