package com.arn.scrobble.db

import androidx.room.*
import com.arn.scrobble.App
import com.arn.scrobble.db.CachedAlbum.Companion.toCachedAlbum
import com.arn.scrobble.db.CachedArtist.Companion.toCachedArtist
import com.arn.scrobble.db.CachedTrack.Companion.toCachedTrack
import com.arn.scrobble.pref.MainPrefs
import de.umass.lastfm.Track


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

    @get:Query("SELECT count(1) FROM $tableName")
    val count: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: List<CachedTrack>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(entry: CachedTrack)

    fun deltaUpdate(track: CachedTrack, deltaCount: Int, dirty: DirtyUpdate = DirtyUpdate.CLEAN) {
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

    @Delete
    fun delete(entry: CachedTrack)

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {

        suspend fun deltaUpdateAll(
            track: Track,
            deltaCount: Int,
            mode: DirtyUpdate = DirtyUpdate.CLEAN
        ) {
            val prefs = MainPrefs(App.context)

            val maxIndexedScrobbleTime = prefs.lastMaxIndexedScrobbleTime ?: -1
            val wasIndexed =
                track.playedWhen != null && track.playedWhen.time < maxIndexedScrobbleTime

            val mode = if (mode == DirtyUpdate.BOTH && !wasIndexed)
                DirtyUpdate.DIRTY
            else
                mode

            if (maxIndexedScrobbleTime > 0 && (wasIndexed || mode == DirtyUpdate.DIRTY)) {
                val db = PanoDb.db
                db.getCachedTracksDao().deltaUpdate(track.toCachedTrack(), deltaCount, mode)
                if (!track.album.isNullOrEmpty()) {
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