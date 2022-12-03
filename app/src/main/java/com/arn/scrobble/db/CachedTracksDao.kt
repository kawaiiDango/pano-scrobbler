package com.arn.scrobble.db

import android.content.Context
import androidx.room.*
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

    fun deltaUpdate(track: CachedTrack, deltaCount: Int) {
        val foundTrack = findExact(track.artistName, track.trackName)

        if (foundTrack != null) {
            foundTrack.userPlayCount = (foundTrack.userPlayCount + deltaCount).coerceAtLeast(0)
            if (track.lastPlayed > -1) {
                foundTrack.lastPlayed = track.lastPlayed
                foundTrack.isLoved = track.isLoved
            }
            update(foundTrack)
        } else if (deltaCount > 0) {
            track.userPlayCount = 1
            insert(listOf(track))
        }
    }

    @Delete
    fun delete(entry: CachedTrack)

    @Query("DELETE FROM $tableName")
    fun nuke()

    companion object {

        suspend fun deltaUpdateAll(
            context: Context,
            track: Track,
            deltaCount: Int,
        ) {
            val prefs = MainPrefs(context)

            val maxIndexedScrobbleTime = prefs.lastMaxIndexedScrobbleTime ?: -1

            if (maxIndexedScrobbleTime > 0 && track.playedWhen != null && track.playedWhen.time < maxIndexedScrobbleTime) {
                val db = PanoDb.getDb(context)
                db.getCachedTracksDao().deltaUpdate(track.toCachedTrack(), deltaCount)
                if (!track.album.isNullOrEmpty()) {
                    db.getCachedAlbumsDao().deltaUpdate(track.toCachedAlbum(), deltaCount)
                }
                db.getCachedArtistsDao().deltaUpdate(track.toCachedArtist(), deltaCount)
            }
        }

        const val tableName = "cachedTracks"
    }
}
