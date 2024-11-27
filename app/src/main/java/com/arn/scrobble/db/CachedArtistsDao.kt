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
interface CachedArtistsDao {

    @Query("SELECT * FROM $tableName WHERE artistName like '%' || :term || '%' ORDER BY userPlayCount DESC LIMIT :limit")
    fun find(term: String, limit: Int = 50): List<CachedArtist>

    @Query("SELECT * FROM $tableName WHERE artistName like :artistName LIMIT 1")
    fun findExact(artistName: String): CachedArtist?

    @Query("SELECT count(1) FROM $tableName")
    fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: List<CachedArtist>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(entry: CachedArtist)

    @Delete
    fun delete(entry: CachedArtist)

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {
        const val tableName = "cachedArtists"

        fun CachedArtistsDao.deltaUpdate(
            artist: CachedArtist,
            deltaCount: Int,
            dirty: DirtyUpdate = DirtyUpdate.CLEAN,
        ) {
            val foundArtist = findExact(artist.artistName) ?: artist

            if (dirty == DirtyUpdate.DIRTY_ABSOLUTE && foundArtist.userPlayCount == artist.userPlayCount) return

            val userPlayCount =
                (foundArtist.userPlayCount.coerceAtLeast(0) + deltaCount).coerceAtLeast(0)
            val userPlayCountDirty = (
                    (if (foundArtist.userPlayCountDirty == -1) foundArtist.userPlayCount else foundArtist.userPlayCountDirty)
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
                    newUserPlayCountDirty = artist.userPlayCount
                    newUserPlayCount = userPlayCount
                }

                DirtyUpdate.CLEAN -> {
                    newUserPlayCount = userPlayCount
                    newUserPlayCountDirty = -1
                }
            }

            val newArtist = foundArtist.copy(
                userPlayCount = newUserPlayCount,
                userPlayCountDirty = newUserPlayCountDirty
            )
            insert(listOf(newArtist))
        }

    }
}
