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

    @Delete
    fun delete(entry: CachedAlbum)

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {
        const val tableName = "cachedAlbums"

        fun CachedAlbumsDao.deltaUpdate(
            album: CachedAlbum,
            deltaCount: Int,
            dirty: DirtyUpdate = DirtyUpdate.CLEAN
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

            when (dirty) {
                DirtyUpdate.BOTH -> {
                    foundAlbum.userPlayCountDirty = userPlayCount
                    foundAlbum.userPlayCount = userPlayCountDirty
                }

                DirtyUpdate.DIRTY -> {
                    foundAlbum.userPlayCountDirty = userPlayCountDirty
                }

                DirtyUpdate.DIRTY_ABSOLUTE -> {
                    foundAlbum.userPlayCountDirty = album.userPlayCount
                }

                DirtyUpdate.CLEAN -> {
                    foundAlbum.userPlayCount = userPlayCount
                    foundAlbum.userPlayCountDirty = -1
                }
            }

            if (foundAlbum.largeImageUrl == null) {
                foundAlbum.largeImageUrl = album.largeImageUrl
            }
            insert(listOf(foundAlbum))
        }
    }
}
