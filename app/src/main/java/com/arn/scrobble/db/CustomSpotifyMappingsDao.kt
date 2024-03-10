package com.arn.scrobble.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomSpotifyMappingsDao {
    @Query("SELECT count(1) FROM $tableName")
    fun count(): Flow<Int>

    @Query(
        """
        SELECT * FROM $tableName
        WHERE artist = :artist and album = ""
        ORDER BY _id DESC LIMIT 1
    """
    )
    fun searchArtist(artist: String): CustomSpotifyMapping?

    @Query(
        """
        SELECT * FROM $tableName
        WHERE artist = :artist and album = :album
        ORDER BY _id DESC LIMIT 1
    """
    )
    fun searchAlbum(artist: String, album: String): CustomSpotifyMapping?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(e: List<CustomSpotifyMapping>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(e: List<CustomSpotifyMapping>)

    @Delete
    fun delete(e: CustomSpotifyMapping)

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {
        const val tableName = "customSpotifyMappings"
    }
}