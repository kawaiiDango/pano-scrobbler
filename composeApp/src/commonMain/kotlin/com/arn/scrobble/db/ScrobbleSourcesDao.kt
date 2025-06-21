package com.arn.scrobble.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arn.scrobble.utils.Stuff

@Dao
interface ScrobbleSourcesDao {
    @Query("SELECT * FROM $tableName ORDER BY _id DESC")
    suspend fun all(): List<ScrobbleSource>

    @Query("SELECT * FROM $tableName WHERE timeMillis >= :earliest AND timeMillis <= :latest ORDER BY timeMillis ASC")
    suspend fun selectBetween(earliest: Long, latest: Long): List<ScrobbleSource>

    @Query("SELECT count(1) FROM $tableName")
    suspend fun count(): Int

    @Query("SELECT * FROM $tableName WHERE ABS(timeMillis - :time) < ${Stuff.SCROBBLE_SOURCE_THRESHOLD} ORDER BY ABS(timeMillis - :time) ASC LIMIT 1")
    suspend fun findPlayer(time: Long): ScrobbleSource?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(p: ScrobbleSource)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(p: List<ScrobbleSource>)

    @Query("DELETE FROM $tableName")
    suspend fun nuke()

    companion object {
        const val tableName = "scrobbleSources"
    }
}