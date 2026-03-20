package com.arn.scrobble.db


import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
interface SeenEntitiesDao {

    // -------------------------------------------------------------------------
    // Normalization helpers — all writes go through these
    // -------------------------------------------------------------------------

    private fun String.norm() = this.trim()

    // -------------------------------------------------------------------------
    // Raw insert / upsert primitives (internal use)
    // -------------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrackIgnore(track: SeenTrack): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAlbumIgnore(album: SeenAlbum): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceAssociation(assoc: SeenTrackAlbumAssociation)

    // -------------------------------------------------------------------------
    // ID resolution helpers
    // -------------------------------------------------------------------------

    @Query("SELECT _id FROM ${SeenTrack.TABLE} WHERE artist = :artist AND track = :track")
    suspend fun getTrackId(artist: String, track: String): Long?

    @Query("SELECT _id FROM ${SeenAlbum.TABLE} WHERE artist = :artist AND album = :album")
    suspend fun getAlbumId(artist: String, album: String): Long?

    /**
     * Returns the existing track id, or inserts a minimal row and returns the
     * new id. Never returns null.
     */
    suspend fun resolveTrackId(artist: String, track: String): Long {
        val existing = getTrackId(artist, track)
        if (existing != null) return existing
        val newId = insertTrackIgnore(SeenTrack(artist = artist, track = track))
        return newId
    }

    /**
     * Returns the existing album id, or inserts a minimal row and returns the
     * new id. Never returns null.
     */
    suspend fun resolveAlbumId(artist: String, album: String): Long {
        val existing = getAlbumId(artist, album)
        if (existing != null) return existing
        val newId = insertAlbumIgnore(SeenAlbum(artist = artist, album = album))
        return newId
    }

    // -------------------------------------------------------------------------
    // Public write API — called from API hook sites
    // -------------------------------------------------------------------------

    /**
     * Saves a track→album association.
     * Only overwrites an existing association if [priority] is >= the stored one.
     * Call this from: track.getInfo, album.getInfo, user.getRecentTracks,
     * media player scrobble hooks.
     */
    @Transaction
    suspend fun saveTrackAlbumAssociation(
        artist: String,
        track: String,
        albumArtist: String,
        album: String,
        artUrl: String? = "",
        priority: SeenTrackAlbumAssociation.Priority,
        seenAt: Long = System.currentTimeMillis(),
    ) {
        val normArtist = artist.norm()
        val normTrack = track.norm()
        val normAlbumArtist = albumArtist.norm()
        val normAlbum = album.norm()

        val trackId = resolveTrackId(normArtist, normTrack)
        val albumId = resolveAlbumId(normAlbumArtist, normAlbum)

        // Update art URL if it is not the sentinel value (empty string)
        if (artUrl != null)
            updateAlbumArt(albumId, artUrl)

        // Only write the association if incoming priority is >= stored priority.
        val stored = getAssociationPriority(trackId, albumId)
        if (stored == null || priority.n >= stored) {
            replaceAssociation(
                SeenTrackAlbumAssociation(
                    trackId = trackId,
                    albumId = albumId,
                    priority = priority.n,
                    seenAt = seenAt,
                )
            )
        }
    }

    /**
     * Saves or updates the loved state for a track.
     * Call this from: user.getLovedTracks, user.getRecentTracks,
     * local love/unlove actions.
     *
     * [updatedAt] should be the current time on local actions, or the
     * timestamp from the API response when syncing from the network.
     */
    @Transaction
    suspend fun saveLovedState(
        artist: String,
        track: String,
        isLoved: Boolean,
        updatedAt: Long = System.currentTimeMillis(),
    ) {
        val normArtist = artist.norm()
        val normTrack = track.norm()
        val trackId = resolveTrackId(normArtist, normTrack)
        updateLovedState(trackId, isLoved, updatedAt)
    }

    /**
     * Records that a track.getInfo call was attempted for this artist+track,
     * regardless of whether the response contained an album.
     * Call this unconditionally after every track.getInfo call.
     */
    @Transaction
    suspend fun markTrackInfoFetched(
        artist: String,
        track: String,
        fetchedAt: Long = System.currentTimeMillis(),
    ) {
        val normArtist = artist.norm()
        val normTrack = track.norm()
        val trackId = resolveTrackId(normArtist, normTrack)
        updateTrackInfoFetchedAt(trackId, fetchedAt)
    }

    /**
     * Convenience: saves association + loved state in one transaction.
     * Suitable for processing a single user.getRecentTracks item, which
     * provides all of these fields at once.
     */
    @Transaction
    suspend fun saveRecentTrack(
        artist: String,
        track: String,
        albumArtist: String,
        album: String,
        artUrl: String? = "",
        isLoved: Boolean?,
        priority: SeenTrackAlbumAssociation.Priority,
        seenAt: Long = System.currentTimeMillis(),
    ) {
        saveTrackAlbumAssociation(
            artist = artist,
            track = track,
            albumArtist = albumArtist,
            album = album,
            artUrl = artUrl,
            priority = priority,
            seenAt = seenAt,
        )

        if (isLoved != null)
            saveLovedState(
                artist = artist,
                track = track,
                isLoved = isLoved,
                updatedAt = seenAt,
            )
    }

    @Transaction
    suspend fun saveAlbumArtIfMissing(
        artist: String,
        album: String,
        artUrl: String,
    ) {
        val normArtist = artist.norm()
        val normAlbum = album.norm()
        val albumId = resolveAlbumId(normArtist, normAlbum)
        updateAlbumArt(albumId, artUrl)
    }

    // -------------------------------------------------------------------------
    // Public read API — lookups
    // -------------------------------------------------------------------------

    /**
     * Returns the highest-priority [SeenAlbum] for the given artist+track pair,
     * or null if no association exists yet.
     */
    @Query(
        """
        SELECT sa.*
        FROM ${SeenAlbum.TABLE} sa
        INNER JOIN ${SeenTrackAlbumAssociation.TABLE} taa ON taa.albumId = sa._id
        INNER JOIN ${SeenTrack.TABLE} st ON st._id = taa.trackId
        WHERE st.artist = :artist AND st.track = :track
        ORDER BY taa.priority ASC, taa.seenAt DESC
        LIMIT 1
    """
    )
    suspend fun getBestAlbumForTrack(artist: String, track: String): SeenAlbum?

    @Query(
        """
        SELECT sa.*
        FROM ${SeenAlbum.TABLE} sa
        INNER JOIN ${SeenTrackAlbumAssociation.TABLE} taa ON taa.albumId = sa._id
        INNER JOIN ${SeenTrack.TABLE} st ON st._id = taa.trackId
        WHERE st.artist = :artist AND st.track = :track AND sa.artUpdatedAt IS NOT NULL
        ORDER BY taa.priority ASC, taa.seenAt DESC
        LIMIT 1
    """
    )
    suspend fun getBestAlbumForTrackWithFetchedArt(artist: String, track: String): SeenAlbum?

    /**
     * Returns the art URL for the given album, or null if unknown / not yet fetched.
     */
    @Query(
        """
        SELECT *
        FROM ${SeenAlbum.TABLE}
        WHERE artist = :artist AND album = :album AND artUpdatedAt IS NOT NULL
        LIMIT 1
    """
    )
    suspend fun getAlbumWithFetchedArt(artist: String, album: String): SeenAlbum?

    /**
     * Returns the full [SeenTrack] row if it exists and has a known loved state,
     * null otherwise.
     * Callers should check [SeenTrack.isLoved] — it is non-null when this returns.
     */
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

    /**
     * Returns the full [SeenTrack] regardless of loved state — useful for
     * checking [SeenTrack.trackInfoFetchedAt] before deciding to call track.getInfo.
     */
    @Query("SELECT * FROM ${SeenTrack.TABLE} WHERE artist = :artist AND track = :track LIMIT 1")
    suspend fun getTrack(artist: String, track: String): SeenTrack?

    // -------------------------------------------------------------------------
    // Internal targeted update queries
    // -------------------------------------------------------------------------

    @Query(
        """
        UPDATE ${SeenTrack.TABLE}
        SET isLoved = :isLoved, lovedStateUpdatedAt = :updatedAt
        WHERE _id = :trackId
    """
    )
    suspend fun updateLovedState(
        trackId: Long,
        isLoved: Boolean,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE ${SeenTrack.TABLE}
        SET trackInfoFetchedAt = :fetchedAt
        WHERE _id = :trackId
    """
    )
    suspend fun updateTrackInfoFetchedAt(trackId: Long, fetchedAt: Long)

    @Query(
        """
        UPDATE ${SeenAlbum.TABLE}
        SET artUrl = :artUrl, artUpdatedAt = :updatedAt
        WHERE _id = :albumId AND artUrl IS NULL
    """
    )
    suspend fun updateAlbumArt(
        albumId: Long,
        artUrl: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        SELECT priority FROM ${SeenTrackAlbumAssociation.TABLE}
        WHERE trackId = :trackId AND albumId = :albumId
    """
    )
    suspend fun getAssociationPriority(trackId: Long, albumId: Long): Int?
}