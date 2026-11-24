package com.arn.scrobble.api.atproto

import androidx.collection.LruCache
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.ScrobbleResult
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.cache.CacheStrategy
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.ImagesUrls
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.PageAttr
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.charts.ListeningActivity
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.cacheStrategy
import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.readString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.URI
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlin.coroutines.cancellation.CancellationException

val AccountType.playCollection: String
    get() = when (this) {
        AccountType.TEAL -> "fm.teal.feed.play"
        AccountType.ROCKSKY -> "app.rocksky.scrobble"
        else -> error("Not an AT Protocol history source")
    }

/** Submit completed plays to the verified PDS. Public history reads don't use the session. */
class AtprotoScrobblable internal constructor(
    account: UserAccountSerializable,
    private val client: () -> HttpClient,
    private val writeClient: () -> HttpClient,
    private val sessions: () -> AtprotoSessions,
) : Scrobblable(account) {
    constructor(account: UserAccountSerializable, client: () -> HttpClient = { Requesters.atprotoPublicRecentsClient }) :
        this(account, client, { Requesters.atprotoPdsClient }, { AtprotoSessions() })
    private val collection = account.type.playCollection
    private val pds = requireNotNull(account.apiRoot).trimEnd('/')
    private val did = account.authKey
    private val recordPrefix = "at://$did/$collection/"
    private val lock = Mutex()
    // The account (DID and PDS) and collection are implicit in this instance.
    private val queries = LruCache<Query, History>(4)

    // Keep the preview and live cursor chains independent; both read bodies through HttpCache.
    private data class Query(val from: Long, val to: Long, val limit: Int, val cached: Boolean)

    private class History {
        val nextCursors = mutableListOf<String?>()
        val seenCursors = mutableMapOf<String, Int>()
        val seenRecords = mutableMapOf<String, Int>()

        fun advance(page: Int, next: String?, cursors: Set<String>, records: Set<String>) {
            // A re-read may change the chain. Discard that page's old metadata and any descendants.
            if (page <= nextCursors.size) {
                nextCursors.subList(page - 1, nextCursors.size).clear()
                seenCursors.values.removeAll { it >= page }
                seenRecords.values.removeAll { it >= page }
            }
            nextCursors += next
            cursors.forEach { seenCursors[it] = page }
            records.forEach { seenRecords[it] = page }
        }
    }

    override suspend fun getRecents(
        page: Int,
        username: String,
        cached: Boolean,
        from: Long,
        to: Long,
        includeNowPlaying: Boolean,
        limit: Int,
    ): Result<PageResult<Track>> = try {
        require(username == userAccount.user.name) { "AT Protocol history is only available for the connected account" }
        require(page >= 1)
        val query = Query(from, to, limit.takeIf { it > 0 }?.coerceAtMost(100) ?: 50, cached)
        Result.success(lock.withLock {
            val history = if (page == 1) History() else queries[query]
            if (history == null || page - 1 > history.nextCursors.size) {
                throw if (cached) ApiException(504, "AT Protocol history is not cached")
                else ApiException(-1, "Reload AT Protocol history from the first page")
            }
            readPage(history, query, page).also { queries.put(query, history) }
        })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Scan past filtered/malformed records without confusing a short or empty batch with exhaustion. */
    private suspend fun readPage(history: History, query: Query, page: Int): PageResult<Track> {
        val tracks = mutableListOf<Track>()
        val records = mutableSetOf<String>()
        val cursors = mutableSetOf<String>()
        var cursor = history.nextCursors.getOrNull(page - 2)
        if (page > 1 && cursor == null) return PageResult(PageAttr(page, page, null), tracks)
        val strategy = if (query.cached) CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED else CacheStrategy.NETWORK_ONLY
        repeat(24) {
            val requestCursor = cursor
            val remaining = query.limit - tracks.size
            val response = client().prepareGet("$pds$AT_RECENTS_PATH") {
                parameter("repo", did)
                parameter("collection", collection)
                parameter("limit", remaining)
                requestCursor?.let { parameter("cursor", it) }
                cacheStrategy(strategy)
            }.execute {
                if (!it.status.isSuccess()) throw ApiException(it.status.value, it.status.description)
                val body = it.bodyAsChannel().readBuffer(AT_RECENTS_MAX_BYTES + 1)
                if (body.size > AT_RECENTS_MAX_BYTES) throw ApiException(-1, "AT Protocol history response too large")
                Stuff.myJson.decodeFromString<RecordPage>(body.readString())
            }
            if (response.records.size > remaining)
                throw ApiException(-1, "PDS returned too many AT Protocol records")
            for (entry in response.records) {
                val record = entry as? JsonObject ?: continue
                val uri = record["uri"].text() ?: continue
                if (!uri.startsWith(recordPrefix) || !recordKey.matches(uri.removePrefix(recordPrefix)) ||
                    (history.seenRecords[uri] ?: page) < page || !records.add(uri)) continue
                val track = (record["value"] as? JsonObject)?.toTrack(userAccount.type) ?: continue
                val date = track.date ?: continue
                if ((query.from < 0 || date >= query.from) && (query.to < 0 || date <= query.to))
                    tracks += track.copy(atprotoUri = uri)
            }
            cursor = response.cursor?.takeIf { it.isNotBlank() }
            val next = cursor
            if (next != null && ((history.seenCursors[next] ?: page) < page || !cursors.add(next)))
                throw ApiException(-1, "PDS repeated an AT Protocol history cursor")
            if (next == null || tracks.size == query.limit) {
                history.advance(page, next, cursors, records)
                return PageResult(PageAttr(page, if (next == null) page else page + 1, null), tracks)
            }
        }
        throw ApiException(-1, "PDS history scan could not finish this page")
    }

    override suspend fun updateNowPlaying(scrobbleData: ScrobbleData) = Result.success(ScrobbleResult(false))

    override suspend fun scrobble(scrobbleData: ScrobbleData): Result<ScrobbleResult> = try {
        val rkey = scrobbleData.atprotoRecordKey()
        val record = scrobbleData.atprotoPlay(userAccount.type)
        val http = writeClient()
        sessions().send(userAccount, http) { http.createPlay(it, collection, rkey, record) }
        Result.success(ScrobbleResult(false))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun scrobble(scrobbleDatas: List<ScrobbleData>): Result<ScrobbleResult> {
        for (play in scrobbleDatas) {
            val result = scrobble(play)
            if (result.isFailure) return result
        }
        return Result.success(ScrobbleResult(false))
    }

    override suspend fun loveOrUnlove(track: Track, love: Boolean) = Result.success(ScrobbleResult(false))
    override suspend fun delete(track: Track) = unavailable<Nothing>()
    override suspend fun getLoves(page: Int, username: String, cacheStrategy: CacheStrategy, limit: Int) =
        unavailable<PageResult<Track>>()

    override suspend fun getFriends(page: Int, username: String, cached: Boolean, limit: Int) =
        unavailable<PageResult<User>>()

    override suspend fun loadDrawerData(username: String) = null

    override suspend fun getCharts(
        type: Int, timePeriod: TimePeriod, page: Int, username: String, cacheStrategy: CacheStrategy, limit: Int,
    ) = unavailable<PageResult<MusicEntry>>()

    override suspend fun getListeningActivity(
        timePeriod: TimePeriod, user: UserCached?, cacheStrategy: CacheStrategy,
    ): ListeningActivity =
        throw UnsupportedOperationException("Only Recents is available for this AT Protocol source")

    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Only Recents is available for this AT Protocol source"))

    @Serializable
    private data class RecordPage(val records: List<JsonElement>, val cursor: String? = null)

    private companion object {
        val recordKey = Regex("[a-zA-Z0-9._~:-]{1,512}")
    }
}

private fun JsonElement?.text() = (this as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
private fun JsonObject.names(key: String, field: String) = (get(key) as? JsonArray).orEmpty().mapNotNull {
    (it as? JsonObject)?.get(field).text()
}.joinToString(", ").takeIf { it.isNotEmpty() }

private fun JsonObject.toTrack(type: AccountType): Track? {
    if (get("\$type").text() != type.playCollection) return null
    val teal = type == AccountType.TEAL
    val title = get(if (teal) "trackName" else "title").text() ?: return null
    val artist = if (teal) {
        names("artists", "artistName") ?: (get("artistNames") as? JsonArray).orEmpty()
            .mapNotNull { it.text() }.joinToString(", ").takeIf { it.isNotEmpty() }
    } else {
        get("artist").text() ?: names("artists", "name")
    }
    val playedAt = get(if (teal) "playedTime" else "createdAt").text() ?: return null
    val date = try {
        Instant.parse(playedAt).toEpochMilli()
    } catch (_: DateTimeParseException) {
        return null
    } catch (_: ArithmeticException) {
        return null
    }
    val duration = (get("duration") as? JsonPrimitive)?.longOrNull?.takeIf { it > 0 }
    val artUrl = if (teal) null else get("albumArtUrl").text()?.takeIf { raw ->
        runCatching { URI(raw) }.getOrNull()?.let { it.scheme == "https" && it.host != null && it.rawUserInfo == null } == true
    }
    return Track(
        name = title,
        artist = Artist(artist.orEmpty()),
        album = get(if (teal) "releaseName" else "album").text()?.let { name ->
            Album(name, artist = if (teal) null else get("albumArtist").text()?.let { Artist(it) },
                image = artUrl?.let { ImagesUrls(medium = it, large = it) })
        },
        duration = if (teal) duration?.takeIf { it <= Long.MAX_VALUE / 1000 }?.times(1000) else duration,
        date = date,
    )
}
