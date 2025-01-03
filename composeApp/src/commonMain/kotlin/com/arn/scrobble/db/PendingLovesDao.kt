package com.arn.scrobble.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


/**
 * Created by arn on 11/09/2017.
 */

@Dao
interface PendingLovesDao {
    @Query("SELECT * FROM $tableName ORDER BY _id DESC LIMIT :limit")
    suspend fun all(limit: Int): List<PendingLove>

    @Query("SELECT * FROM $tableName ORDER BY _id DESC LIMIT :limit")
    fun allFlow(limit: Int): Flow<List<PendingLove>>

    @Query("SELECT * FROM $tableName WHERE artist =:artist AND track=:track")
    suspend fun find(artist: String, track: String): PendingLove?

    @Query("SELECT count(1) FROM $tableName")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pl: PendingLove)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(pl: PendingLove)

    @Query("UPDATE $tableName SET state = state & :loggedInAccountsBitset")
    suspend fun removeLoggedOutAccounts(loggedInAccountsBitset: Int)

    @Delete
    suspend fun delete(pl: PendingLove)

    @Query("DELETE FROM $tableName WHERE state = 0")
    suspend fun deleteStateZero()

    @Query("DELETE FROM $tableName")
    suspend fun nuke()

    companion object {
        const val tableName = "PendingLoves"
    }
}
