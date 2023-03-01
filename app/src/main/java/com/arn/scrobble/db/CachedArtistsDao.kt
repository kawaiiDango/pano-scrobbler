package com.arn.scrobble.db

import androidx.room.*


/**
 * Created by arn on 11/09/2017.
 */

@Dao
interface CachedArtistsDao {

    @Query("SELECT * FROM $tableName WHERE artistName like '%' || :term || '%' ORDER BY userPlayCount DESC LIMIT :limit")
    fun find(term: String, limit: Int = 50): List<CachedArtist>

    @Query("SELECT * FROM $tableName WHERE artistName like :artistName LIMIT 1")
    fun findExact(artistName: String): CachedArtist?

    @get:Query("SELECT count(1) FROM $tableName")
    val count: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: List<CachedArtist>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(entry: CachedArtist)

    fun deltaUpdate(artist: CachedArtist, deltaCount: Int, dirty: DirtyUpdate = DirtyUpdate.CLEAN) {
        val foundArtist = findExact(artist.artistName) ?: artist

        if (dirty == DirtyUpdate.DIRTY_ABSOLUTE && foundArtist.userPlayCount == artist.userPlayCount) return

        val userPlayCount =
            (foundArtist.userPlayCount.coerceAtLeast(0) + deltaCount).coerceAtLeast(0)
        val userPlayCountDirty = (
                (if (foundArtist.userPlayCountDirty == -1) foundArtist.userPlayCount else foundArtist.userPlayCountDirty)
                    .coerceAtLeast(0)
                        + deltaCount
                ).coerceAtLeast(0)

        when (dirty) {
            DirtyUpdate.BOTH -> {
                foundArtist.userPlayCountDirty = userPlayCount
                foundArtist.userPlayCount = userPlayCountDirty
            }

            DirtyUpdate.DIRTY -> {
                foundArtist.userPlayCountDirty = userPlayCountDirty
            }

            DirtyUpdate.DIRTY_ABSOLUTE -> {
                foundArtist.userPlayCountDirty = artist.userPlayCount
            }

            DirtyUpdate.CLEAN -> {
                foundArtist.userPlayCount = userPlayCount
                foundArtist.userPlayCountDirty = -1
            }
        }
        insert(listOf(foundArtist))
    }

    @Delete
    fun delete(entry: CachedArtist)

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {
        const val tableName = "cachedArtists"
    }
}
