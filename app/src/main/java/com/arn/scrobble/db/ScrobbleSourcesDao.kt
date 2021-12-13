package com.arn.scrobble.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

private const val tableName = "scrobbleSources"
private const val THRESHOLD = 1000 // ms

@Dao
interface ScrobbleSourcesDao {
    @get:Query("SELECT * FROM $tableName ORDER BY _id DESC")
    val all: List<ScrobbleSource>

    @get:Query("SELECT count(1) FROM $tableName")
    val count: Int

    @Query("SELECT * FROM $tableName WHERE ABS(timeMillis - :timeMillis) < $THRESHOLD ORDER BY ABS(timeMillis - :timeMillis) ASC LIMIT 1")
    fun findPlayer(timeMillis: Long): ScrobbleSource?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(p: ScrobbleSource)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(p: List<ScrobbleSource>)

    @Query("DELETE FROM $tableName")
    fun nuke()
}