package com.arn.scrobble.db

import androidx.room.*


/**
 * Created by arn on 11/09/2017.
 */

@Dao
interface CachedAlbumsDao {

    @Query("SELECT * FROM $tableName WHERE artistName like '%' || :term || '%' OR albumName like '%' || :term || '%' ORDER BY userPlayCount DESC LIMIT :limit")
    fun find(term: String, limit: Int = 50): List<CachedAlbum>

    @Query("SELECT * FROM $tableName WHERE artistName like :artistName AND albumName like :albumName LIMIT 1")
    fun findExact(artistName: String, albumName: String): CachedAlbum?

    @get:Query("SELECT count(1) FROM $tableName")
    val count: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: List<CachedAlbum>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(entry: CachedAlbum)

    fun deltaUpdate(album: CachedAlbum, deltaCount: Int) {
        val foundAlbum = findExact(album.artistName, album.albumName)

        if (foundAlbum != null) {
            foundAlbum.userPlayCount = (foundAlbum.userPlayCount + deltaCount).coerceAtLeast(0)
            if (foundAlbum.largeImageUrl == null) {
                foundAlbum.largeImageUrl = album.largeImageUrl
            }
            update(foundAlbum)
        } else if (deltaCount > 0) {
            album.userPlayCount = 1
            insert(listOf(album))
        }
    }

    @Delete
    fun delete(entry: CachedAlbum)

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {
        const val tableName = "cachedAlbums"
    }
}
