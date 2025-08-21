package com.arn.scrobble.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


/**
 * Created by arn on 11/09/2017.
 */

@Dao
interface CachedTracksWithAlbumsDao {
    @Query("SELECT * FROM $tableName WHERE artist = :artist AND track = :track ORDER BY insertedAt DESC LIMIT 1")
    suspend fun find(artist: String, track: String): CachedTrackWithAlbum?

    @Query("SELECT count(1) FROM $tableName")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: List<CachedTrackWithAlbum>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(entry: CachedTrackWithAlbum)

    @Delete
    suspend fun delete(entry: CachedTrackWithAlbum)

    @Query("DELETE FROM $tableName")
    suspend fun nuke()

    companion object {
        const val tableName = "cachedTracksWithAlbums"
    }
}