package com.arn.scrobble.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.CachedAlbum.Companion.toCachedAlbum
import com.arn.scrobble.db.CachedAlbumsDao.Companion.deltaUpdate
import com.arn.scrobble.db.CachedArtist.Companion.toCachedArtist
import com.arn.scrobble.db.CachedArtistsDao.Companion.deltaUpdate
import com.arn.scrobble.db.CachedTrack.Companion.toCachedTrack
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


/**
 * Created by arn on 11/09/2017.
 */

@Dao
interface CachedTracksDao {
    @Query("SELECT * FROM $tableName WHERE (artistName like '%' || :term || '%' OR trackName like '%' || :term || '%') AND isLoved = 0 ORDER BY userPlayCount DESC LIMIT :limit")
    suspend fun findTop(term: String, limit: Int = 50): List<CachedTrack>

    @Query("SELECT * FROM $tableName WHERE (artistName like '%' || :term || '%' OR trackName like '%' || :term || '%') AND isLoved = 1 ORDER BY userPlayCount DESC LIMIT :limit")
    suspend fun findLoved(term: String, limit: Int = 50): List<CachedTrack>

    @Query("SELECT * FROM $tableName WHERE artistName like :artist AND trackName like :track LIMIT 1")
    suspend fun findExact(artist: String, track: String): CachedTrack?

    @Query("SELECT count(1) FROM $tableName")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: List<CachedTrack>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(entry: CachedTrack)


    @Delete
    suspend fun delete(entry: CachedTrack)

    @Query("DELETE FROM $tableName")
    suspend fun nuke()

    companion object {

        suspend fun CachedTracksDao.deltaUpdate(
            track: CachedTrack,
            deltaCount: Int,
            dirty: DirtyUpdate = DirtyUpdate.CLEAN,
        ) {
            val foundTrack = findExact(track.artistName, track.trackName) ?: track

            if (dirty == DirtyUpdate.DIRTY_ABSOLUTE && foundTrack.userPlayCount == track.userPlayCount && foundTrack.isLoved == track.isLoved)
                return

            val userPlayCount =
                (foundTrack.userPlayCount.coerceAtLeast(0) + deltaCount).coerceAtLeast(0)
            val userPlayCountDirty = (
                    (if (foundTrack.userPlayCountDirty == -1) foundTrack.userPlayCount else foundTrack.userPlayCountDirty)
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
                    newUserPlayCountDirty = track.userPlayCount
                    newUserPlayCount = userPlayCount
                }

                DirtyUpdate.CLEAN -> {
                    newUserPlayCount = userPlayCount
                    newUserPlayCountDirty = -1
                }
            }

            val newTrack = foundTrack.copy(
                userPlayCount = newUserPlayCount,
                userPlayCountDirty = newUserPlayCountDirty,
                lastPlayed = if (track.lastPlayed > -1) track.lastPlayed else foundTrack.lastPlayed,
                isLoved = if (track.lastPlayed > -1) track.isLoved else foundTrack.isLoved
            )

            insert(listOf(newTrack))
        }

        suspend fun deltaUpdateAll(
            track: Track,
            deltaCount: Int,
            mode: DirtyUpdate = DirtyUpdate.CLEAN,
        ) {
            val lastMaxIndexedScrobbleTime =
                PlatformStuff.mainPrefs.data.map { it.lastMaxIndexedScrobbleTime }.first()

            val maxIndexedScrobbleTime = lastMaxIndexedScrobbleTime ?: -1
            val wasIndexed =
                track.date != null && track.date < maxIndexedScrobbleTime

            val mode = if (mode == DirtyUpdate.BOTH && !wasIndexed)
                DirtyUpdate.DIRTY
            else
                mode

            if (maxIndexedScrobbleTime > 0 && (wasIndexed || mode == DirtyUpdate.DIRTY)) {
                val db = PanoDb.db
                db.getCachedTracksDao().deltaUpdate(track.toCachedTrack(), deltaCount, mode)
                if (track.album != null) {
                    db.getCachedAlbumsDao().deltaUpdate(track.toCachedAlbum(), deltaCount, mode)
                }
                db.getCachedArtistsDao().deltaUpdate(track.toCachedArtist(), deltaCount, mode)
            }
        }

        const val tableName = "cachedTracks"
    }
}


enum class DirtyUpdate {
    BOTH, DIRTY, DIRTY_ABSOLUTE, CLEAN
}