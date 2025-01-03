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
interface CachedAlbumsDao {

    @Query("SELECT * FROM $tableName WHERE artistName like '%' || :term || '%' OR albumName like '%' || :term || '%' ORDER BY userPlayCount DESC LIMIT :limit")
    suspend fun find(term: String, limit: Int = 50): List<CachedAlbum>

    @Query("SELECT * FROM $tableName WHERE artistName like :artistName AND albumName like :albumName LIMIT 1")
    suspend fun findExact(artistName: String, albumName: String): CachedAlbum?

    @Query("SELECT count(1) FROM $tableName")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: List<CachedAlbum>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(entry: CachedAlbum)

    @Delete
    suspend fun delete(entry: CachedAlbum)

    @Query("DELETE FROM $tableName")
    suspend fun nuke()

    companion object {
        const val tableName = "cachedAlbums"

        suspend fun CachedAlbumsDao.deltaUpdate(
            album: CachedAlbum,
            deltaCount: Int,
            dirty: DirtyUpdate = DirtyUpdate.CLEAN,
        ) {
            val foundAlbum = findExact(album.artistName, album.albumName) ?: album

            if (dirty == DirtyUpdate.DIRTY_ABSOLUTE && foundAlbum.userPlayCount == album.userPlayCount) return

            val userPlayCount =
                (foundAlbum.userPlayCount.coerceAtLeast(0) + deltaCount).coerceAtLeast(0)
            val userPlayCountDirty = (
                    (if (foundAlbum.userPlayCountDirty == -1) foundAlbum.userPlayCount else foundAlbum.userPlayCountDirty)
                        .coerceAtLeast(0)
                            + deltaCount
                    ).coerceAtLeast(0)

            val newUserPlayCountDirty: Int
            val newUserPlayCount: Int
            when (dirty) {
                DirtyUpdate.BOTH -> {
                    newUserPlayCountDirty = userPlayCount
                    newUserPlayCount = userPlayCountDirty
                }

                DirtyUpdate.DIRTY -> {
                    newUserPlayCountDirty = userPlayCountDirty
                    newUserPlayCount = userPlayCount
                }

                DirtyUpdate.DIRTY_ABSOLUTE -> {
                    newUserPlayCountDirty = album.userPlayCount
                    newUserPlayCount = userPlayCount
                }

                DirtyUpdate.CLEAN -> {
                    newUserPlayCount = userPlayCount
                    newUserPlayCountDirty = -1
                }
            }

            val newAlbum = foundAlbum.copy(
                userPlayCount = newUserPlayCount,
                userPlayCountDirty = newUserPlayCountDirty,
                largeImageUrl = foundAlbum.largeImageUrl ?: album.largeImageUrl
            )
            insert(listOf(newAlbum))
        }
    }
}
