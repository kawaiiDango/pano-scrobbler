package com.arn.scrobble.db

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistsWithDelimitersDao {
    @Query("SELECT MAX(_id) FROM $tableName")
    suspend fun maxId(): Int?

    @Query("SELECT * FROM $tableName ORDER BY artist")
    fun allFlow(): Flow<List<ArtistWithDelimiters>>

    @Query("SELECT * FROM $tableName WHERE artist like '%' || :term || '%' ORDER BY artist")
    fun searchPartial(term: String): Flow<List<ArtistWithDelimiters>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: List<ArtistWithDelimiters>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ArtistWithDelimiters)

    @Delete
    suspend fun delete(entry: ArtistWithDelimiters)

    @Query("DELETE FROM $tableName")
    suspend fun nuke()

    companion object {
        const val tableName = "artistsWithDelimiters"
    }
}