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
interface PendingLovesDao {
    @Query("SELECT * FROM $tableName ORDER BY _id DESC LIMIT :limit")
    fun all(limit: Int): List<PendingLove>

    @Query("SELECT * FROM $tableName ORDER BY _id DESC LIMIT :limit")
    fun allLd(limit: Int): LiveData<List<PendingLove>>

    @Query("SELECT * FROM $tableName WHERE artist =:artist AND track=:track")
    fun find(artist: String, track: String): PendingLove?

    @Query("SELECT count(1) FROM $tableName")
    fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(pl: PendingLove)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(pl: PendingLove)

    @Query("UPDATE $tableName SET state = state & :loggedInAccountsBitset")
    fun removeLoggedOutAccounts(loggedInAccountsBitset: Int)

    @Delete
    fun delete(pl: PendingLove)

    @Query("DELETE FROM $tableName WHERE state = 0")
    fun deleteStateZero()

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {
        const val tableName = "PendingLoves"
    }
}
