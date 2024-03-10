package com.arn.scrobble.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

private const val THRESHOLD = 1000 // ms

@Dao
interface ScrobbleSourcesDao {
    @Query("SELECT * FROM $tableName ORDER BY _id DESC")
    fun all(): List<ScrobbleSource>

    @Query("SELECT count(1) FROM $tableName")
    fun count(): Int

    @Query("SELECT * FROM $tableName WHERE ABS(timeMillis - :time) < $THRESHOLD ORDER BY ABS(timeMillis - :time) ASC LIMIT 1")
    fun findPlayer(time: Long): ScrobbleSource?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(p: ScrobbleSource)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(p: List<ScrobbleSource>)

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {
        const val tableName = "scrobbleSources"
    }
}