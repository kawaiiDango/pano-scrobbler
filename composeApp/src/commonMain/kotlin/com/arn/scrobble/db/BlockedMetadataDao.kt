package com.arn.scrobble.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arn.scrobble.api.lastfm.ScrobbleData
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedMetadataDao {
    @Query("SELECT * FROM $tableName ORDER BY _id DESC")
    fun allFlow(): Flow<List<BlockedMetadata>>

    @Query("SELECT count(1) FROM $tableName")
    fun count(): Flow<Int>

    @Query(
        """SELECT * FROM $tableName
          WHERE artist IN ('', :artist) AND
          album IN ('', :album) AND
          albumArtist IN ('', :albumArtist) AND
          track IN ('', :track) AND
          NOT (artist == '' AND album == '' AND albumArtist == '' AND track == '')
    """
    )
    suspend fun getBlockedEntries(
        artist: String,
        album: String,
        albumArtist: String,
        track: String,
    ): List<BlockedMetadata>

    @Query(
        """
        SELECT * FROM $tableName
        WHERE (artist LIKE '%' || :term || '%' OR
        album LIKE '%' || :term || '%' OR
        albumArtist LIKE '%' || :term || '%' OR
        track LIKE '%' || :term || '%')
        ORDER BY _id DESC
    """
    )
    fun searchPartial(term: String): Flow<List<BlockedMetadata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: List<BlockedMetadata>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(e: List<BlockedMetadata>)

    @Delete
    suspend fun delete(e: BlockedMetadata)

    @Query("DELETE FROM $tableName")
    suspend fun nuke()

    companion object {
        const val tableName = "blockedMetadata"

        suspend fun BlockedMetadataDao.getBlockedEntry(
            scrobbleData: ScrobbleData,
        ): BlockedMetadata? {
            val entries = getBlockedEntries(
                scrobbleData.artist.lowercase(),
                scrobbleData.album?.lowercase() ?: "",
                scrobbleData.albumArtist?.lowercase() ?: "",
                scrobbleData.track.lowercase(),
            )

            if (entries.isEmpty()) return null
            if (entries.size == 1) return entries[0]

            // conflict resolution for multiple entries
            val entriesToBlanks = mutableListOf<Pair<BlockedMetadata, Int>>()
            entries.forEach {
                val blanks =
                    arrayOf(it.artist, it.album, it.albumArtist, it.track).count { it.isBlank() }
                entriesToBlanks += it to blanks
            }

            entriesToBlanks.sortBy { it.second }

            val lowestBlanks = entriesToBlanks[0].second
            val maxIdx = entriesToBlanks.indexOfLast { it.second == lowestBlanks }
            val bestEntries = entriesToBlanks.map { it.first }.subList(0, maxIdx + 1)
            val bestEntry = bestEntries.first()
            return bestEntry
        }


        suspend fun BlockedMetadataDao.insertLowerCase(
            entries: List<BlockedMetadata>,
            ignore: Boolean = false,
        ) {
            val lowercaseEntries = entries.map { entry ->
                entry.copy(
                    artist = entry.artist.lowercase(),
                    album = entry.album.lowercase(),
                    albumArtist = entry.albumArtist.lowercase(),
                    track = entry.track.lowercase()
                )
            }
            if (ignore)
                insertIgnore(lowercaseEntries)
            else
                insert(lowercaseEntries)
        }

    }
}