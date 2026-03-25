package com.arn.scrobble.db

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.ScrobbleEvent
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.flow.Flow
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException


@Dao
interface PendingScrobblesDao {
    @Query("SELECT * FROM $tableName ORDER BY timestamp DESC LIMIT :limit")
    fun allFlow(limit: Int): Flow<List<PendingScrobble>>

    @Query("SELECT * FROM $tableName WHERE event = 'scrobble' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun allScrobbles(limit: Int): List<PendingScrobble>

    @Query("SELECT * FROM $tableName WHERE event != 'scrobble' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun allLoves(limit: Int): List<PendingScrobble>

    @Query("SELECT count(1) FROM $tableName")
    suspend fun count(): Int

    @Query("SELECT * FROM $tableName WHERE artist =:artist AND track=:track AND event = :event")
    suspend fun find(artist: String, track: String, event: ScrobbleEvent): PendingScrobble?

    @Query("SELECT * FROM $tableName WHERE artist =:artist AND track=:track AND event != 'scrobble'")
    suspend fun findLoved(artist: String, track: String): PendingScrobble?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIgnore(ps: PendingScrobble)

    suspend fun insert(
        scrobbleData: ScrobbleData,
        event: ScrobbleEvent,
        services: List<AccountType>,
        exceptions: List<Throwable>
    ) {
        val ps = PendingScrobble(
            scrobbleData = scrobbleData,
            event = event,
            services = services.toSet(),
            lastFailedReason = exceptions
                .firstOrNull()
                ?.redactedMessage?.take(100),
            canForceRetry = exceptions.any { it.isNetworkRetryable }
        )

        insertIgnore(ps)
    }

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

    suspend fun logFailure(ids: List<Long>, exceptions: List<Throwable>) {
        logFailure(
            ids,
            System.currentTimeMillis(),
            exceptions.firstOrNull()?.redactedMessage?.take(100),
            exceptions.any { it.isNetworkRetryable }
        )
    }

    @Query("SELECT 1 FROM $tableName WHERE canForceRetry = 1 OR lastFailedTimestamp <= :retryAfterTimestamp")
    suspend fun canForceRetry(
        retryAfterTimestamp: Long = System.currentTimeMillis() - 30 * 60 * 1000L
    ): Boolean

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

        private val Throwable.isNetworkRetryable: Boolean
            get() =
                this is UnknownHostException ||
                        this is SSLException ||
                        this is SocketTimeoutException ||
                        this is SocketException ||
                        // 11, 403 = Access Denied - You cannot access this service
                        this is ApiException && (code == 11 || code == 403 || code == 502)

    }
}
