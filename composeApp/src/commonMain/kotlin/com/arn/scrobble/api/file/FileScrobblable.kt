package com.arn.scrobble.api.file

import androidx.collection.CircularArray
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleIgnored
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.CacheStrategy
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.PageAttr
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Session
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.ui.PackageName
import com.arn.scrobble.ui.PackageNameMetadata.englishLabel
import com.arn.scrobble.ui.PackageNameMetadata.version
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformFile
import com.arn.scrobble.utils.PlatformStuff
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.OutputStream

class FileScrobblable(userAccount: UserAccountSerializable) : Scrobblable(userAccount) {
    private val fileType = FileFormat.valueOf(userAccount.authKey)

    val platformFile = PlatformFile(userAccount.apiRoot!!)

    override suspend fun updateNowPlaying(scrobbleData: ScrobbleData): Result<ScrobbleIgnored> {
        // no op
        return Result.success(ScrobbleIgnored(false))
    }

    override suspend fun scrobble(scrobbleData: ScrobbleData): Result<ScrobbleIgnored> {

        return kotlin.runCatching {
            if (!platformFile.isFileOk())
                throw FException(platformFile, "File not writable")

            platformFile.writeAppend { outputStream ->
                writeEntry(scrobbleData, Event.scrobble, outputStream)
            }

            ScrobbleIgnored(false)
        }
    }

    override suspend fun scrobble(scrobbleDatas: List<ScrobbleData>): Result<ScrobbleIgnored> {
        return kotlin.runCatching {
            if (!platformFile.isFileOk())
                throw FException(platformFile, "File not writable")

            platformFile.writeAppend { outputStream ->
                scrobbleDatas.forEach { scrobbleData ->
                    writeEntry(scrobbleData, Event.scrobble, outputStream)
                }
            }

            ScrobbleIgnored(false)
        }
    }

    private suspend fun writeEntry(
        data: ScrobbleData,
        event: Event,
        outputStream: OutputStream,
    ) {
        val entry = Entry.fromScrobbleData(data, event)
        when (fileType) {
            FileFormat.csv -> {
                withContext(Dispatchers.IO) {
                    csvWriter().writeAllAsync(listOf(entry.toCsvRow()), outputStream)
                }
            }

            FileFormat.jsonl -> {
                withContext(Dispatchers.IO) {
                    outputStream.write((entry.toJson() + "\n").toByteArray())
                }
            }
        }
    }

    override suspend fun loveOrUnlove(track: Track, love: Boolean): Result<ScrobbleIgnored> {
        return kotlin.runCatching {
            if (!platformFile.isFileOk())
                throw FException(platformFile, "File not writable")

            val scrobbleData = ScrobbleData(
                artist = track.artist.name,
                track = track.name,
                album = track.album?.name,
                timestamp = System.currentTimeMillis(),
                albumArtist = track.album?.artist?.name,
                duration = track.duration,
                packageName = null
            )

            val action = if (love) Event.love else Event.unlove

            platformFile.writeAppend { outputStream ->
                writeEntry(scrobbleData, action, outputStream)
            }

            ScrobbleIgnored(false)
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
        limit: Int,
    ): Result<PageResult<Track>> {

        return kotlin.runCatching {
            if (cached)
                throw IllegalStateException("Cache not supported")

            if (!platformFile.isFileOk())
                throw FException(platformFile, "File not writable")

            if (platformFile.length() > MAX_SIZE)
                throw FException(platformFile, "File too large to read")

            val lines = CircularArray<String>(limit)
            var readFirstLine = false

            platformFile.read { inputStream ->
                inputStream.bufferedReader().useLines { sequence ->
                    sequence.forEach { line ->
                        if (!readFirstLine) {
                            readFirstLine = true
                            if (fileType == FileFormat.csv && line.startsWith(Entry::timeHuman.name))
                                return@forEach
                        }

                        if (lines.size() >= limit)
                            lines.popFirst()

                        lines.addLast(line)
                    }
                }
            }

            val csvReader = csvReader()

            val entries = (0 until lines.size())
                .map { lines[it] }
                .mapNotNull {
                    val entry: Entry
                    if (fileType == FileFormat.csv) {
                        val row = csvReader.readAll(it).firstOrNull() ?: return@mapNotNull null
                        entry = Entry.fromCsvRow(row)
                    } else {
                        entry = Entry.fromJson(it)
                    }

                    Track(
                        name = entry.track,
                        artist = Artist(entry.artist),
                        album = entry.album?.let { albumName ->
                            Album(albumName, entry.albumArtist?.let { Artist(it) })
                        },
                        userloved = entry.event == Event.love,
                        userHated = entry.event == Event.unlove,
                        duration = entry.durationMs,
                        date = entry.timeMs
                    )
                }.asReversed()

            PageResult(
                PageAttr(
                    page = 1,
                    totalPages = 1,
                    total = entries.size
                ),
                entries = entries,
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

    override suspend fun loadDrawerData(username: String): DrawerData {
        val isSelf = username == userAccount.user.name

        val dd = DrawerData(0)
        if (isSelf) {
            PlatformStuff.mainPrefs.updateData {
                it.copy(drawerData = it.drawerData + (userAccount.type to dd))
            }
        }

        return dd
    }

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
    ): Map<TimePeriod, Int> {
        // no op
        return emptyMap()
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
        val event: Event,
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
                event = FileScrobblable.Event.valueOf(row.getOrNull(10) ?: Event.scrobble.name)
            )

            fun fromScrobbleData(scrobbleData: ScrobbleData, event: Event) = Entry(
                timeHuman = PanoTimeFormatter.full(scrobbleData.timestamp),
                timeMs = scrobbleData.timestamp,
                artist = scrobbleData.artist,
                track = scrobbleData.track,
                album = scrobbleData.album,
                albumArtist = scrobbleData.albumArtist,
                durationMs = scrobbleData.duration,
                mediaPlayerPackage = scrobbleData.packageName,
                mediaPlayerName = scrobbleData.packageName?.let { PackageName(it).englishLabel },
                mediaPlayerVersion = scrobbleData.packageName?.let { PackageName(it).version },
                event = event
            )
        }
    }

    private enum class Event {
        scrobble, love, unlove
    }

    enum class FileFormat {
        csv, jsonl
    }

    class FException(val platformFile: PlatformFile, message: String, cause: Throwable? = null) :
        IOException(message, cause)

    companion object {
        private const val MAX_SIZE = 10 * 1024 * 1024L // 10MB

        private val CSV_HEADER =
            "${Entry::timeHuman.name},${Entry::timeMs.name},${Entry::artist.name},${Entry::track.name},${Entry::album.name},${Entry::albumArtist.name},${Entry::durationMs.name},${Entry::mediaPlayerPackage.name},${Entry::mediaPlayerName.name},${Entry::mediaPlayerVersion.name},${Entry::event.name}"

        suspend fun authAndGetSession(
            platformFile: PlatformFile,
            format: FileFormat,
        ): Result<Session> {
            return kotlin.runCatching {
                val fileName = platformFile.getFileName()

                require(fileName.lowercase().endsWith(".${format.name}")) {
                    "File name must end with .${format.name}"
                }

                platformFile.takePersistableUriPermission(readWrite = true)

                if (format == FileFormat.csv && platformFile.length() == 0L) {
                    platformFile.overwrite { outputStream ->
                        outputStream.bufferedWriter().use {
                            it.write(CSV_HEADER + "\n")
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
                Session(fileName, format.name)
            }
        }
    }

}