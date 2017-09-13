package com.arn.scrobble.db

import android.arch.persistence.room.*


/**
 * Created by arn on 11/09/2017.
 */
const val tableName = "pendingScrobbles"

@Dao
interface PendingScrobblesDao {
    @get:Query("SELECT * FROM $tableName")
    val all: List<PendingScrobble>

    @get:Query("SELECT * FROM $tableName WHERE autoCorrected = 0")
    val allNotAutocorrected: List<PendingScrobble>

    @get:Query("SELECT * FROM $tableName WHERE autoCorrected = 1")
    val allAutocorrected: List<PendingScrobble>

    @get:Query("SELECT * FROM $tableName LIMIT 1")
    val loadLastPending: PendingScrobble

    @get:Query("SELECT count(*) FROM $tableName")
    val count: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg ps: PendingScrobble)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(ps: PendingScrobble)

    @Delete
    fun delete(ps: PendingScrobble)
}
