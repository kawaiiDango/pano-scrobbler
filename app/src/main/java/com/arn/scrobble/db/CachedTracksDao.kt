package com.arn.scrobble.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arn.scrobble.App
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.CachedAlbum.Companion.toCachedAlbum
import com.arn.scrobble.db.CachedAlbumsDao.Companion.deltaUpdate
import com.arn.scrobble.db.CachedArtist.Companion.toCachedArtist
import com.arn.scrobble.db.CachedArtistsDao.Companion.deltaUpdate
import com.arn.scrobble.db.CachedTrack.Companion.toCachedTrack


/**
 * Created by arn on 11/09/2017.
 */

@Dao
interface CachedTracksDao {
    @Query("SELECT * FROM $tableName WHERE (artistName like '%' || :term || '%' OR trackName like '%' || :term || '%') AND isLoved = 0 ORDER BY userPlayCount DESC LIMIT :limit")
    fun findTop(term: String, limit: Int = 50): List<CachedTrack>

    @Query("SELECT * FROM $tableName WHERE (artistName like '%' || :term || '%' OR trackName like '%' || :term || '%') AND isLoved = 1 ORDER BY userPlayCount DESC LIMIT :limit")
    fun findLoved(term: String, limit: Int = 50): List<CachedTrack>

    @Query("SELECT * FROM $tableName WHERE artistName like :artist AND trackName like :track LIMIT 1")
    fun findExact(artist: String, track: String): CachedTrack?

    @Query("SELECT count(1) FROM $tableName")
    fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: List<CachedTrack>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(entry: CachedTrack)


    @Delete
    fun delete(entry: CachedTrack)

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {

        fun CachedTracksDao.deltaUpdate(
            track: CachedTrack,
            deltaCount: Int,
            dirty: DirtyUpdate = DirtyUpdate.CLEAN
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

            when (dirty) {
                DirtyUpdate.BOTH -> {
                    foundTrack.userPlayCountDirty = userPlayCount
                    foundTrack.userPlayCount = userPlayCountDirty
                }

                DirtyUpdate.DIRTY -> {
                    foundTrack.userPlayCountDirty = userPlayCountDirty
                }

                DirtyUpdate.DIRTY_ABSOLUTE -> {
                    foundTrack.userPlayCountDirty = track.userPlayCount
                }

                DirtyUpdate.CLEAN -> {
                    foundTrack.userPlayCount = userPlayCount
                    foundTrack.userPlayCountDirty = -1
                }
            }

            if (track.lastPlayed > -1) {
                foundTrack.lastPlayed = track.lastPlayed
                foundTrack.isLoved = track.isLoved
            }

            insert(listOf(foundTrack))
        }

        suspend fun deltaUpdateAll(
            track: Track,
            deltaCount: Int,
            mode: DirtyUpdate = DirtyUpdate.CLEAN
        ) {
            val prefs = App.prefs

            val maxIndexedScrobbleTime = prefs.lastMaxIndexedScrobbleTime ?: -1
            val wasIndexed =
                track.date != null && track.date < maxIndexedScrobbleTime / 1000

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