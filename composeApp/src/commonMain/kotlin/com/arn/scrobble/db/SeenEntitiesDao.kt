package com.arn.scrobble.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300

private fun String.norm() = this.trim()

/**
 * Stable separator that must not appear in any artist/track/album name.
 * U+001F UNIT SEPARATOR — a C0 control character
 */
private const val SEP = "\u001F"

// SQLite has a default max of 999 variables per statement
// Use a safe lower number to allow for any additional parameters in the queries
private const val VAR_LIMIT = 990

private fun idKey(artist: String, second: String) = "$artist$SEP$second"

// public API function names start with "save" or "get" only
@Dao
interface SeenEntitiesDao {
    data class CompositeIdRow(
        val _id: Long,
        val compositeKey: String,
    )

    data class AssociationPriorityRow(
        val compositeKey: String,
        val priority: Int,
    )

    // -------------------------------------------------------------------------
    // Raw insert primitives (internal use only)
    // -------------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracksIgnore(tracks: List<SeenTrack>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbumsIgnore(albums: List<SeenAlbum>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAssociations(assocs: List<SeenTrackAlbumAssociation>)


    // -------------------------------------------------------------------------
    // ID resolution helpers — batch (used by batch hooks)
    // -------------------------------------------------------------------------

    /**
     * Returns (_id, artist||SEP||track) pairs for any rows already in the table.
     * We use string concatenation in SQL so Room can bind a single-column IN list
     * of composite keys. The separator must not appear in the data.
     */
    @Query(
        """
        SELECT _id, artist || '$SEP' || track AS compositeKey
        FROM ${SeenTrack.TABLE}
        WHERE artist || '$SEP' || track IN (:keys)
        """
    )
    suspend fun lookupTrackIdsForKeys(keys: List<String>): List<CompositeIdRow>

    @Query(
        """
        SELECT _id, artist || '$SEP' || album AS compositeKey
        FROM ${SeenAlbum.TABLE}
        WHERE artist || '$SEP' || album IN (:keys)
        """
    )
    suspend fun lookupAlbumIdsForKeys(keys: List<String>): List<CompositeIdRow>

    /**
     * Resolves IDs for a whole batch of (artist, second) pairs.
     *
     * Strategy (2 round-trips instead of 2N):
     *   1. INSERT OR IGNORE all rows that don't exist yet (one statement).
     *   2. SELECT all IDs by composite key (one statement).
     *
     * Returns a map of idKey(artist, second) → _id.
     */
    suspend fun resolveTrackIds(pairs: List<Pair<String, String>>): Map<String, Long> {
        if (pairs.isEmpty()) return emptyMap()
        val distinct = pairs.distinctBy { idKey(it.first, it.second) }
        insertTracksIgnore(distinct.map { SeenTrack(artist = it.first, track = it.second) })
        val keys = distinct.map { idKey(it.first, it.second) }
        return keys.chunked(VAR_LIMIT)
            .flatMap { lookupTrackIdsForKeys(it) }
            .associate { it.compositeKey to it._id }
    }

    suspend fun resolveAlbumIds(pairs: List<Pair<String, String>>): Map<String, Long> {
        if (pairs.isEmpty()) return emptyMap()
        val distinct = pairs.distinctBy { idKey(it.first, it.second) }
        insertAlbumsIgnore(distinct.map { SeenAlbum(artist = it.first, album = it.second) })
        val keys = distinct.map { idKey(it.first, it.second) }
        return keys.chunked(VAR_LIMIT)
            .flatMap { lookupAlbumIdsForKeys(it) }
            .associate { it.compositeKey to it._id }
    }

    // -------------------------------------------------------------------------
    // Association priority — batch fetch
    // -------------------------------------------------------------------------

    /**
     * Fetches existing priorities for a set of (trackId, albumId) pairs in one
     * query. Room can't bind a list of tuples
     */
    @Query(
        """
        SELECT trackId || '$SEP' || albumId AS compositeKey, priority
        FROM ${SeenTrackAlbumAssociation.TABLE}
        WHERE trackId || '$SEP' || albumId IN (:keys)
        """
    )
    suspend fun lookupAssociationPriorities(keys: List<String>): List<AssociationPriorityRow>

    // -------------------------------------------------------------------------
    // Public write API — batch hooks
    // -------------------------------------------------------------------------

    /**
     * Processes one full page of user.getRecentTracks / user.getTopTracks /
     * track.getSimilar / user.getWeeklyTrackChart in a single transaction.
     *
     * Round-trips (for a page of N tracks):
     *   New: 2 INSERTs + 2 SELECTs + 1 priority SELECT + 1 REPLACE batch = 6 (constant)
     */
    @Transaction
    suspend fun saveRecentTracks(
        items: List<Track>,
        mayHaveAlbumArt: Boolean,
        savedLoved: Boolean,
        priority: SeenTrackAlbumAssociation.Priority,
    ) {
        if (items.isEmpty()) return
        val now = System.currentTimeMillis()

        // Collect only items that have an album name — skip tracks with no album.
        data class Resolved(
            val trackId: Long,
            val albumId: Long,
            val artUrl: String?,
            val isLoved: Boolean?,
        )

        val trackPairs = items.map { it.artist.name.norm() to it.name.norm() }
        val albumPairs = items.mapNotNull { item ->
            val albumName = item.album?.name?.norm() ?: return@mapNotNull null
            val albumArtist = (item.album.artist?.name ?: item.artist.name).norm()
            albumArtist to albumName
        }

        val trackIds = resolveTrackIds(trackPairs)
        val albumIds = resolveAlbumIds(albumPairs)

        if (priority == SeenTrackAlbumAssociation.Priority.TRACK_INFO) {
            trackIds.values.distinct().chunked(VAR_LIMIT).forEach {
                updateTrackInfoFetchedAt(it, now)
            }
        }

        // Album art: batch-update all at once via individual SQL calls still,
        // but only for items that actually have art (see note below).
        // Resolve all association priorities in one query.
        val resolved = mutableListOf<Resolved>()
        for (item in items) {
            val albumName = item.album?.name?.norm() ?: continue
            val albumArtist = (item.album.artist?.name ?: item.artist.name).norm()

            val trackId = trackIds[idKey(item.artist.name.norm(), item.name.norm())] ?: continue
            val albumId = albumIds[idKey(albumArtist, albumName)] ?: continue

            resolved.add(Resolved(trackId, albumId, item.album.webp300, item.userloved))
        }

        // updateAlbumArtIfMissing is still per-row SQL

        if (mayHaveAlbumArt) {
            val artUpdates = resolved.distinctBy { it.albumId }
            for (r in artUpdates) {
                updateAlbumArtIfMissing(r.albumId, r.artUrl)
            }
        }

        // Batch priority lookup.
        val assocKeys = resolved.map { "${it.trackId}$SEP${it.albumId}" }
        val storedPriorities = assocKeys.chunked(VAR_LIMIT)
            .flatMap { lookupAssociationPriorities(it) }
            .associate { it.compositeKey to it.priority }

        // Build the associations to write.
        val toReplace = resolved.mapNotNull { r ->
            val stored = storedPriorities["${r.trackId}$SEP${r.albumId}"]
            if (stored == null || priority.n <= stored) {
                SeenTrackAlbumAssociation(
                    trackId = r.trackId,
                    albumId = r.albumId,
                    priority = priority.n,
                    seenAt = now,
                )
            } else null
        }
        if (toReplace.isNotEmpty()) replaceAssociations(toReplace)

        // Loved state: batch update for items that have a known loved state.
        if (!savedLoved) return

        saveLovedTracks(items, now, trackIds)
    }

    /**
     * Processes one full page of user.getLovedTracks in a single transaction.
     *
     * Round-trips: 2 (batch INSERT + batch SELECT) + 1 UPDATE IN = 3 constant.
     */
    suspend fun saveLovedTracks(
        items: List<Track>,
        updatedAt: Long = System.currentTimeMillis(),
        trackIdsp: Map<String, Long>? = null,
    ) {
        if (items.isEmpty()) return

        val trackIds = trackIdsp
            ?: resolveTrackIds(items.map { it.artist.name.norm() to it.name.norm() })

        val (loved, notLoved) = items.mapNotNull { item ->
            val isLoved = item.userloved ?: return@mapNotNull null
            val trackId = trackIds[idKey(item.artist.name.norm(), item.name.norm())]
                ?: return@mapNotNull null
            trackId to isLoved
        }
            .partition { (_, isLoved) -> isLoved }

        loved.map { it.first }.chunked(VAR_LIMIT).forEach {
            updateLovedStateForIds(it, true, updatedAt)
        }
        notLoved.map { it.first }.chunked(VAR_LIMIT).forEach {
            updateLovedStateForIds(it, false, updatedAt)
        }
    }

    /**
     * Stores album art URLs from user.getTopAlbums in a single transaction.
     *
     * Round-trips: 2 (batch INSERT + batch SELECT) + per-distinct-album UPDATE = constant + small.
     */
    @Transaction
    suspend fun saveTopAlbumArts(items: List<Album>) {
        val filtered = items.filter { it.artist != null && !it.webp300.isNullOrEmpty() }
        if (filtered.isEmpty()) return

        val pairs = filtered.map { it.artist!!.name.norm() to it.name.norm() }
        val albumIds = resolveAlbumIds(pairs)

        for (item in filtered) {
            val albumId = albumIds[idKey(item.artist!!.name.norm(), item.name.norm())] ?: continue
            updateAlbumArtIfMissing(albumId, item.webp300)
        }
    }

    /**
     * Stores the tracklist from album.getInfo in a single transaction.
     *
     * Round-trips: 1 album resolve + 1 batch track INSERT + 1 batch track SELECT
     *            + 1 priority batch SELECT + 1 REPLACE batch = 5 constant.
     */
    @Transaction
    suspend fun saveAlbumInfoTracklist(
        album: Album,
        seenAt: Long = System.currentTimeMillis(),
    ) {
        val normAlbumArtist = album.artist?.name?.norm() ?: return
        val normAlbum = album.name.norm()
        val tracks = album.tracks?.track.orEmpty()
        if (tracks.isEmpty()) return

        val albumId =
            resolveAlbumIds(listOf(normAlbumArtist to normAlbum)).values.firstOrNull() ?: return

        updateAlbumArtIfMissing(albumId, album.webp300)

        val trackPairs = tracks.map { it.artist.name.norm() to it.name.norm() }
        val trackIds = resolveTrackIds(trackPairs).map { (_, id) -> id }

        trackIds.chunked(VAR_LIMIT).forEach {
            updateTrackInfoFetchedAt(it, seenAt)
        }

        val priority = SeenTrackAlbumAssociation.Priority.ALBUM_INFO.n
        val assocKeys = trackIds.map { "${it}$SEP${albumId}" }
        val storedPriorities = assocKeys.chunked(VAR_LIMIT)
            .flatMap { lookupAssociationPriorities(it) }
            .associate { it.compositeKey to it.priority }

        val toReplace = trackIds.mapNotNull { trackId ->
            val stored = storedPriorities["${trackId}$SEP${albumId}"]
            if (stored == null || priority <= stored) {
                SeenTrackAlbumAssociation(
                    trackId = trackId,
                    albumId = albumId,
                    priority = priority,
                    seenAt = seenAt,
                )
            } else null
        }
        if (toReplace.isNotEmpty()) replaceAssociations(toReplace)
    }

    // -------------------------------------------------------------------------
    // Public read API
    // -------------------------------------------------------------------------

    /**
     * Returns the highest-priority [SeenAlbum] for the given artist+track pair.
     * Lower priority number = more trusted (MEDIA_PLAYER = 10 wins).
     */
    @Query(
        """
        SELECT sa.*
        FROM ${SeenAlbum.TABLE} sa
        INNER JOIN ${SeenTrackAlbumAssociation.TABLE} taa ON taa.albumId = sa._id
        INNER JOIN ${SeenTrack.TABLE} st ON st._id = taa.trackId
        WHERE st.artist = :artist AND st.track = :track
        ORDER BY taa.priority ASC, taa.seenAt DESC
        """
    )
    suspend fun getBestAlbumsForTrack(artist: String, track: String): List<SeenAlbum>

    @Query(
        """
        SELECT *
        FROM ${SeenAlbum.TABLE}
        WHERE artist = :artist AND album = :album AND artUpdatedAt IS NOT NULL
        LIMIT 1
        """
    )
    suspend fun getAlbumWithFetchedArt(artist: String, album: String): SeenAlbum?

    @Query(
        """
        SELECT *
        FROM ${SeenTrack.TABLE}
        WHERE artist = :artist AND track = :track
          AND isLoved IS NOT NULL
        LIMIT 1
        """
    )
    suspend fun getTrackWithLovedState(artist: String, track: String): SeenTrack?

    @Query("SELECT * FROM ${SeenTrack.TABLE} WHERE artist = :artist AND track = :track LIMIT 1")
    suspend fun getTrack(artist: String, track: String): SeenTrack?

    // -------------------------------------------------------------------------
    // Internal targeted update queries
    // -------------------------------------------------------------------------

    /**
     * Batch version for when all items in the batch share the same loved state.
     * Used by saveLovedTracks (all items are loved = true).
     */
    @Query(
        """
        UPDATE ${SeenTrack.TABLE}
        SET isLoved = :isLoved, lovedStateUpdatedAt = :updatedAt
        WHERE _id IN (:trackIds)
        """
    )
    suspend fun updateLovedStateForIds(trackIds: List<Long>, isLoved: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE ${SeenTrack.TABLE}
        SET trackInfoFetchedAt = :fetchedAt
        WHERE _id IN (:trackId) AND trackInfoFetchedAt IS NULL
        """
    )
    suspend fun updateTrackInfoFetchedAt(trackId: List<Long>, fetchedAt: Long)

    /**
     * artUpdatedAt is always written so the TTL clock advances even when there
     * is no art (artUrl = null means "valid album, no art available").
     * Only updates when artUrl IS NULL to avoid downgrading a better URL.
     */
    @Query(
        """
        UPDATE ${SeenAlbum.TABLE}
        SET artUrl = :artUrl, artUpdatedAt = :updatedAt
        WHERE _id = :albumId AND (artUrl IS NULL OR artUpdatedAt IS NULL)
        """
    )
    suspend fun updateAlbumArtIfMissing(
        albumId: Long,
        artUrl: String?,
        updatedAt: Long = System.currentTimeMillis(),
    )
}