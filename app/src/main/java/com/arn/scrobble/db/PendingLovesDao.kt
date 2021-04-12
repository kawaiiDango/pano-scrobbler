package com.arn.scrobble.db

import androidx.room.*


/**
 * Created by arn on 11/09/2017.
 */
private const val tableName = "pendingLoves"

@Dao
interface PendingLovesDao {
    @Query("SELECT * FROM $tableName ORDER BY _id DESC LIMIT :limit")
    fun all(limit: Int): List<PendingLove>

    @Query("SELECT * FROM $tableName WHERE artist =:artist AND track=:track")
    fun find(artist: String, track: String): PendingLove?

    @get:Query("SELECT count(1) FROM $tableName")
    val count: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(pl: PendingLove)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(pl: PendingLove)

    @Delete
    fun delete(pl: PendingLove)
}
