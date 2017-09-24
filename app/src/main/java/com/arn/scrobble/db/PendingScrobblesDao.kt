package com.arn.scrobble.db

import android.arch.persistence.room.*


/**
 * Created by arn on 11/09/2017.
 */
const val tableName = "pendingScrobbles"

@Dao
interface PendingScrobblesDao {
    @Query("SELECT * FROM $tableName ORDER BY timestamp DESC LIMIT :limit")
    fun all(limit: Int): List<PendingScrobble>

    @Query("SELECT * FROM $tableName WHERE autoCorrected = 1 LIMIT :limit")
    fun allAutocorrected(limit: Int): List<PendingScrobble>

    @Query("SELECT * FROM $tableName WHERE autoCorrected = 0 LIMIT :limit")
    fun allNotAutocorrected(limit: Int): List<PendingScrobble>

    @get:Query("SELECT * FROM $tableName WHERE autoCorrected = 0 LIMIT 1")
    val loadLastPending: PendingScrobble

    @get:Query("SELECT count(*) FROM $tableName")
    val count: Int

    @get:Query("SELECT count(*) FROM $tableName WHERE autoCorrected = 1")
    val allAutocorrectedCount: Int

    @get:Query("SELECT count(*) FROM $tableName WHERE autoCorrected = 0")
    val allNotAutocorrectedCount: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg ps: PendingScrobble)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(ps: PendingScrobble)

    @Delete
    fun delete(ps: PendingScrobble)
}
