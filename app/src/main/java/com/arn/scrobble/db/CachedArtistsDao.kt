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

    fun deltaUpdate(artist: CachedArtist, deltaCount: Int) {
        val foundArtist = findExact(artist.artistName)

        if (foundArtist != null) {
            foundArtist.userPlayCount = (foundArtist.userPlayCount + deltaCount).coerceAtLeast(0)
            update(foundArtist)
        } else if (deltaCount > 0) {
            artist.userPlayCount = 1
            insert(listOf(artist))
        }
    }

    @Delete
    fun delete(entry: CachedArtist)

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {
        const val tableName = "cachedArtists"
    }
}
