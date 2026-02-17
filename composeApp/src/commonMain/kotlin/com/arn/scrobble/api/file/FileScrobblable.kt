package com.arn.scrobble.api.file

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleEvent
import com.arn.scrobble.api.ScrobbleResult
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.cache.CacheStrategy
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.PageAttr
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.charts.ListeningActivity
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.ui.PackageName
import com.arn.scrobble.ui.PackageNameMetadata.englishLabel
import com.arn.scrobble.ui.PackageNameMetadata.version
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformFile
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.OutputStream

class FileScrobblable(userAccount: UserAccountSerializable) : Scrobblable(userAccount) {
    private val fileType = FileFormat.valueOf(userAccount.authKey)

    private val platformFile = PlatformFile(userAccount.apiRoot!!)
    private val csvReader by lazy { csvReader() }
    private val csvWriter by lazy { csvWriter() }

    override suspend fun updateNowPlaying(scrobbleData: ScrobbleData): Result<ScrobbleResult> {
        // no op
        return Result.success(ScrobbleResult(false))
    }

    override suspend fun scrobble(scrobbleData: ScrobbleData): Result<ScrobbleResult> {

        return kotlin.runCatching {
            if (!platformFile.isWritable())
                throw FException("File not writable")

            platformFile.writeAppend { outputStream ->
                writeEntry(scrobbleData, ScrobbleEvent.scrobble, outputStream)
            }

            ScrobbleResult(false)
        }
    }

    override suspend fun scrobble(scrobbleDatas: List<ScrobbleData>): Result<ScrobbleResult> {
        return kotlin.runCatching {
            if (!platformFile.isWritable())
                throw FException("File not writable")

            platformFile.writeAppend { outputStream ->
                scrobbleDatas.forEach { scrobbleData ->
                    writeEntry(scrobbleData, ScrobbleEvent.scrobble, outputStream)
                }
            }

            ScrobbleResult(false)
        }
    }

    private suspend fun writeEntry(
        data: ScrobbleData,
        event: ScrobbleEvent,
        outputStream: OutputStream,
    ) {
        val entry = Entry.fromScrobbleData(data, event)
        when (fileType) {
            FileFormat.csv -> {
                withContext(Dispatchers.IO) {
                    csvWriter.writeAllAsync(listOf(entry.toCsvRow()), outputStream)
                }
            }

            FileFormat.jsonl -> {
                withContext(Dispatchers.IO) {
                    outputStream.write((entry.toJson() + "\n").toByteArray())
                }
            }
        }
    }

    override suspend fun loveOrUnlove(track: Track, love: Boolean): Result<ScrobbleResult> {
        return kotlin.runCatching {
            if (!platformFile.isWritable())
                throw FException("File not writable")

            val scrobbleData = ScrobbleData(
                artist = track.artist.name,
                track = track.name,
                album = track.album?.name,
                timestamp = System.currentTimeMillis(),
                albumArtist = track.album?.artist?.name,
                duration = track.duration,
                appId = null
            )

            val action = if (love) ScrobbleEvent.love else ScrobbleEvent.unlove

            platformFile.writeAppend { outputStream ->
                writeEntry(scrobbleData, action, outputStream)
            }

            ScrobbleResult(false)
        }
    }

    override suspend fun delete(track: Track): Result<Unit> {
        // no op
        return Result.success(Unit)
    }

    override suspend fun getRecents(
        page: Int,
        username: String,
        cached: Boolean,
        from: Long,
        to: Long,
        includeNowPlaying: Boolean,
        limit: Int, // ignore the limit for files, we go by max bytes instead
    ): Result<PageResult<Track>> {

        return kotlin.runCatching {
            if (cached)
                throw IllegalStateException("Cache not supported")

            if (!platformFile.isWritable())
                throw FException("File not writable")

            val entries = mutableListOf<Track>()

            var readFirstLine = false

            platformFile.readLastNBytes(MAX_SIZE) { inputStream, nExceedsLength ->
                inputStream.bufferedReader().useLines { sequence ->
                    sequence.forEach { line ->
                        if (!readFirstLine) {
                            readFirstLine = true

                            // if lengthExceeds, the first line is probably incomplete, so discard it
                            if (nExceedsLength ||
                                fileType == FileFormat.csv && line.startsWith(Entry::timeHuman.name)
                            )
                                return@forEach
                        }

                        val entry: Entry
                        if (fileType == FileFormat.csv) {
                            val row = csvReader.readAll(line).firstOrNull() ?: return@forEach
                            entry = Entry.fromCsvRow(row)
                        } else {
                            entry = try {
                                Entry.fromJson(line)
                            } catch (e: SerializationException) {
                                return@forEach
                            }
                        }

                        entries += Track(
                            name = entry.track,
                            artist = Artist(entry.artist),
                            album = entry.album?.let { albumName ->
                                Album(albumName, entry.albumArtist?.let { Artist(it) })
                            },
                            userloved = entry.event == ScrobbleEvent.love,
                            userHated = entry.event == ScrobbleEvent.unlove,
                            duration = entry.durationMs,
                            date = entry.timeMs,
                            appId = entry.mediaPlayerPackage,
                        )
                    }
                }
            }

            PageResult(
                PageAttr(
                    page = 1,
                    totalPages = 1,
                    total = entries.size
                ),
                entries = entries.asReversed(),
            )
        }
    }

    override suspend fun getLoves(
        page: Int,
        username: String,
        cacheStrategy: CacheStrategy,
        limit: Int,
    ): Result<PageResult<Track>> {
        // no op
        return Result.success(createEmptyPageResult())
    }

    override suspend fun getFriends(
        page: Int,
        username: String,
        cached: Boolean,
        limit: Int,
    ): Result<PageResult<User>> {
        // no op
        return Result.success(createEmptyPageResult())
    }

    override suspend fun loadDrawerData(username: String) = null

    override suspend fun getCharts(
        type: Int,
        timePeriod: TimePeriod,
        page: Int,
        username: String,
        cacheStrategy: CacheStrategy,
        limit: Int,
    ): Result<PageResult<out MusicEntry>> {
        // no op
        return Result.success(createEmptyPageResult())
    }

    override suspend fun getListeningActivity(
        timePeriod: TimePeriod,
        user: UserCached?,
        cacheStrategy: CacheStrategy,
    ): ListeningActivity {
        // no op
        return ListeningActivity()
    }

    suspend fun convert(fromFile: PlatformFile, fromType: FileFormat) {
        if (!platformFile.isWritable() || !fromFile.isWritable())
            throw FException("File not writable")

        val fileName = fromFile.name()

        require(fileName.lowercase().endsWith(".${fromType.name}")) {
            "\"$fileName\" must end with .${fromType.name}"
        }

        require(fromType != fileType) { "Source and destination file types are the same" }

        platformFile.writeAppend { outputStream ->
            fromFile.read { inputStream ->
                var readFirstLine = false
                inputStream.bufferedReader().useLines { sequence ->
                    sequence.forEach { line ->
                        if (!readFirstLine) {
                            readFirstLine = true

                            if (fromType == FileFormat.csv && line.startsWith(Entry::timeHuman.name))
                                return@forEach
                        }

                        if (fromType == FileFormat.csv && fileType == FileFormat.jsonl) {
                            val row = csvReader.readAll(line).firstOrNull() ?: return@forEach
                            val entry = Entry.fromCsvRow(row)
                            val convertedLine = entry.toJson() + "\n"

                            outputStream.write(convertedLine.toByteArray())

                        } else if (fromType == FileFormat.jsonl && fileType == FileFormat.csv) {
                            try {
                                val entry = Entry.fromJson(line)
                                val row = entry.toCsvRow()
                                val convertedLine = csvWriter.writeAllAsString(listOf(row))

                                outputStream.write(convertedLine.toByteArray())
                            } catch (e: SerializationException) {
                                return@forEach
                            }
                        }
                    }
                }
            }
        }
    }

    @Serializable
    private data class Entry(
        val timeHuman: String,
        val timeMs: Long,
        val artist: String,
        val track: String,
        val album: String?,
        val albumArtist: String?,
        val durationMs: Long?,
        val mediaPlayerPackage: String? = null,
        val mediaPlayerName: String? = null,
        val mediaPlayerVersion: String? = null,
        val event: ScrobbleEvent,
    ) {
        fun toJson() = Json.encodeToString(this)

        fun toCsvRow() = listOf(
            timeHuman,
            timeMs.toString(),
            artist,
            track,
            album,
            albumArtist,
            durationMs.toString(),
            mediaPlayerPackage,
            mediaPlayerName,
            mediaPlayerVersion,
            event.name
        )

        companion object {
            fun fromJson(json: String) = Json.decodeFromString<Entry>(json)

            fun fromCsvRow(row: List<String>) = Entry(
                timeHuman = row[0],
                timeMs = row[1].toLong(),
                artist = row[2],
                track = row[3],
                album = row[4],
                albumArtist = row[5],
                durationMs = row[6].toLongOrNull(),
                mediaPlayerPackage = row[7],
                mediaPlayerName = row.getOrNull(8),
                mediaPlayerVersion = row.getOrNull(9),
                event = ScrobbleEvent.valueOf(row.getOrNull(10) ?: ScrobbleEvent.scrobble.name)
            )

            fun fromScrobbleData(scrobbleData: ScrobbleData, event: ScrobbleEvent) = Entry(
                timeHuman = PanoTimeFormatter.full(scrobbleData.timestamp),
                timeMs = scrobbleData.timestamp,
                artist = scrobbleData.artist,
                track = scrobbleData.track,
                album = scrobbleData.album,
                albumArtist = scrobbleData.albumArtist,
                durationMs = scrobbleData.duration,
                mediaPlayerPackage = scrobbleData.appId,
                mediaPlayerName = scrobbleData.appId?.let { PackageName(it).englishLabel },
                mediaPlayerVersion = scrobbleData.appId?.let { PackageName(it).version },
                event = event
            )
        }
    }

    enum class FileFormat {
        csv, jsonl
    }

    class FException(message: String, cause: Throwable? = null) :
        IOException(message, cause)

    companion object {
        private const val MAX_SIZE = 1 * 1024 * 1024L // 1MB

        private val CSV_HEADER =
            "${Entry::timeHuman.name},${Entry::timeMs.name},${Entry::artist.name},${Entry::track.name},${Entry::album.name},${Entry::albumArtist.name},${Entry::durationMs.name},${Entry::mediaPlayerPackage.name},${Entry::mediaPlayerName.name},${Entry::mediaPlayerVersion.name},${Entry::event.name}"

        suspend fun authAndGetAccount(
            platformFile: PlatformFile,
            format: FileFormat,
        ): Result<UserAccountSerializable> {
            return kotlin.runCatching {
                val fileName = platformFile.name()

                require(fileName.lowercase().endsWith(".${format.name}")) {
                    "\"$fileName\" must end with .${format.name}"
                }

                platformFile.takePersistableUriPermission(readWrite = true)

                // always trigger a write to create the file if it doesn't exist
                if (platformFile.length() == 0L) {
                    platformFile.overwrite { outputStream ->
                        if (format == FileFormat.csv) {
                            outputStream.bufferedWriter().use {
                                it.write(CSV_HEADER + "\n")
                            }
                        }
                    }
                }

                val account = UserAccountSerializable(
                    AccountType.FILE,
                    UserCached(
                        fileName,
                        platformFile.uri,
                        fileName,
                        "",
                        platformFile.lastModified().takeIf { it > 0 } ?: System.currentTimeMillis(),
                    ),
                    format.name,
                    platformFile.uri,
                )

                Scrobblables.add(account)
                account
            }
        }
    }

}