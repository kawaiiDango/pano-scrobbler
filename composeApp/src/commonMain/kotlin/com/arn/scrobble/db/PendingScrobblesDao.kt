package com.arn.scrobble.db

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.arn.scrobble.api.ScrobbleEvent
import kotlinx.coroutines.flow.Flow


/**
 * Created by arn on 11/09/2017.
 */

@Dao
interface PendingScrobblesDao {
    @Query("SELECT * FROM $tableName ORDER BY timestamp DESC LIMIT :limit")
    fun allFlow(limit: Int): Flow<List<PendingScrobble>>

    @Query("SELECT * FROM $tableName WHERE event = 'scrobble' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun allScrobbles(limit: Int): List<PendingScrobble>

    @Query("SELECT * FROM $tableName WHERE event != 'scrobble' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun allLoves(limit: Int): List<PendingScrobble>

    @Query("SELECT * FROM $tableName WHERE artist =:artist AND track=:track AND event = :event")
    suspend fun find(artist: String, track: String, event: ScrobbleEvent): PendingScrobble?

    @Query("SELECT * FROM $tableName WHERE artist =:artist AND track=:track AND event != 'scrobble'")
    suspend fun findLoved(artist: String, track: String): PendingScrobble?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ps: PendingScrobble)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(ps: PendingScrobble)

    @Query("UPDATE $tableName SET services = services & ~:loggedOutAccountsBitset WHERE services & :loggedOutAccountsBitset != 0")
    suspend fun removeLoggedOutAccounts(loggedOutAccountsBitset: Int)

    @Query("UPDATE $tableName SET lastFailedTimestamp = :lastFailedTimestamp, lastFailedReason = :lastFailedReason, canForceRetry = :canForceRetry WHERE _id IN (:ids)")
    suspend fun logFailure(
        ids: List<Long>,
        lastFailedTimestamp: Long,
        lastFailedReason: String?,
        canForceRetry: Boolean
    )

    @Query("SELECT MAX(lastFailedTimestamp) FROM $tableName WHERE canForceRetry = 0")
    suspend fun lastFailedTimestamp(): Long?

    @Delete
    suspend fun delete(ps: PendingScrobble)

    @Query("DELETE FROM $tableName WHERE _id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("DELETE FROM $tableName WHERE services = 0")
    suspend fun deleteEmptyAccounts()

    @Query("DELETE FROM $tableName")
    suspend fun nuke()

    companion object {
        const val tableName = "PendingScrobbles"
    }
}
