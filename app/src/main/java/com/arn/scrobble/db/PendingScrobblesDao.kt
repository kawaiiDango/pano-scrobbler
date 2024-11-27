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
interface PendingScrobblesDao {
    @Query("SELECT * FROM $tableName ORDER BY timestamp DESC LIMIT :limit")
    fun allFlow(limit: Int): Flow<List<PendingScrobble>>

    @Query("SELECT count(1) FROM $tableName")
    fun count(): Int

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
