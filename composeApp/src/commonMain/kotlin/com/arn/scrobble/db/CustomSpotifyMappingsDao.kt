package com.arn.scrobble.db

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomSpotifyMappingsDao {
    @Query("SELECT count(1) FROM $tableName")
    fun count(): Flow<Int>

    @Query(
        """
        SELECT * FROM $tableName
        WHERE artist = :artist and album = ''
        ORDER BY _id DESC LIMIT 1
    """
    )
    suspend fun searchArtist(artist: String): CustomSpotifyMapping?

    @Query(
        """
        SELECT * FROM $tableName
        WHERE artist = :artist and album = :album
        ORDER BY _id DESC LIMIT 1
    """
    )
    suspend fun searchAlbum(artist: String, album: String): CustomSpotifyMapping?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: List<CustomSpotifyMapping>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(e: List<CustomSpotifyMapping>)

    @Delete
    suspend fun delete(e: CustomSpotifyMapping)

    @Query("DELETE FROM $tableName")
    suspend fun nuke()

    companion object {
        const val tableName = "customSpotifyMappings"
    }
}