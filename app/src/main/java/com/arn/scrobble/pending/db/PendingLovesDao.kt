package com.arn.scrobble.pending.db

import androidx.room.*


/**
 * Created by arn on 11/09/2017.
 */
private const val tableName = "pendingLoves"

@Dao
interface PendingLovesDao {
    @Query("SELECT * FROM $tableName LIMIT :limit")
    fun all(limit: Int): List<PendingLove>

    @get:Query("SELECT * FROM $tableName LIMIT 1")
    val loadLastPending: PendingLove?

    @Query("SELECT * FROM $tableName WHERE artist =:artist AND track=:track")
    fun find(artist: String, track: String): PendingLove?

    @get:Query("SELECT count(*) FROM $tableName")
    val count: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg pl: PendingLove)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(pl: PendingLove)

    @Delete
    fun delete(pl: PendingLove)
}
