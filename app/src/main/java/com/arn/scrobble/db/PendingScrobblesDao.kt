package com.arn.scrobble.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


/**
 * Created by arn on 11/09/2017.
 */

@Dao
interface PendingScrobblesDao {
    @Query("SELECT * FROM $tableName ORDER BY _id DESC LIMIT :limit")
    fun all(limit: Int): List<PendingScrobble>

    @Query("SELECT * FROM $tableName ORDER BY _id DESC LIMIT :limit")
    fun allLd(limit: Int): LiveData<List<PendingScrobble>>

    @Query("SELECT * FROM $tableName WHERE (album = \"\" OR albumArtist = artist OR albumArtist = \"\") AND autoCorrected = 0 ORDER BY _id DESC LIMIT :limit")
    fun allEmptyAlbumORAlbumArtist(limit: Int): List<PendingScrobble>

//    @Query("SELECT * FROM $tableName WHERE autoCorrected = 1 ORDER BY _id DESC LIMIT :limit")
//    fun allAutocorrected(limit: Int): List<PendingScrobble>

//    @Query("SELECT * FROM $tableName WHERE autoCorrected = 0 ORDER BY _id DESC LIMIT :limit")
//    fun allNotAutocorrected(limit: Int): List<PendingScrobble>

//    @Query("UPDATE $tableName SET autoCorrected = 1 WHERE ARTIST = :artist")
//    fun markValidArtist(artist: String)

//    @Query("DELETE FROM $tableName WHERE ARTIST = :artist")
//    fun deleteInvalidArtist(artist: String)

//    @get:Query("SELECT * FROM $tableName WHERE autoCorrected = 0 LIMIT 1")
//    val oneNotAutocorrected: PendingScrobble?

    @get:Query("SELECT count(1) FROM $tableName")
    val count: Int

    @Query("SELECT count(1) FROM $tableName WHERE autoCorrected = :autoCorrected")
    fun getAutoCorrectedCount(autoCorrected: Boolean): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ps: PendingScrobble)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(ps: PendingScrobble)

    @Query("UPDATE $tableName SET state = state & :loggedInAccountsBitset")
    fun removeLoggedOutAccounts(loggedInAccountsBitset: Int)

    @Delete
    fun delete(ps: PendingScrobble)

    @Query("DELETE FROM $tableName WHERE _id IN (:ids)")
    fun delete(ids: List<Int>)

    @Query("DELETE FROM $tableName WHERE state = 0")
    fun deleteStateZero()

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {
        const val tableName = "PendingScrobbles"
    }
}
