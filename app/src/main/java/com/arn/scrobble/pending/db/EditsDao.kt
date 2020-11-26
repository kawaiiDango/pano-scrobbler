package com.arn.scrobble.pending.db

import androidx.room.*


/**
 * Created by arn on 11/09/2017.
 */
private const val tableName = "edits"

@Dao
interface EditsDao {
    @get:Query("SELECT * FROM $tableName")
    val all: List<Edit>

    @Query("SELECT * FROM $tableName WHERE (origArtist = :artist and origAlbum = :album and origTrack = :track) OR legacyHash = :hash")
    fun findByNamesOrHash(artist: String, album: String, track: String, hash: String): Edit?

    fun find(artist: String, album: String, track: String): Edit? {
        val hash = if (artist == "" && track != "")
            track.hashCode().toString() + album.hashCode().toString() + artist.hashCode().toString()
        else
            artist.hashCode().toString() + album.hashCode().toString() + track.hashCode().toString()
        return findByNamesOrHash(artist.toLowerCase(), album.toLowerCase(), track.toLowerCase(), hash)
    }

    @get:Query("SELECT count(1) FROM $tableName")
    val count: Int

    fun insertReplaceLowerCase(e: Edit) {
        e.origArtist = e.origArtist.toLowerCase()
        e.origAlbum = e.origAlbum.toLowerCase()
        e.origTrack = e.origTrack.toLowerCase()
        insert(e)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(e: Edit)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(e: Edit)

    @Delete
    fun delete(e: Edit)

    @Query("DELETE FROM $tableName WHERE legacyHash = :hash")
    fun deleteLegacy(hash: String)

    @Query("DELETE FROM $tableName")
    fun nuke()
}
