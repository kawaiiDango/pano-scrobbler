package com.arn.scrobble.api.teal

import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.*
import com.arn.scrobble.api.cache.CacheStrategy
import com.arn.scrobble.api.lastfm.*
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.api.listenbrainz.ListenBrainzMbidResolver
import com.arn.scrobble.charts.ListeningActivity
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.utils.PlatformStuff
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*
import java.io.File
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class Teal internal constructor(
    userAccount: UserAccountSerializable,
    sessionStore: TealSessionStore? = null,
    httpClient: HttpClient? = null,
) : Scrobblable(userAccount) {
    private val usePersistedAccounts = sessionStore == null
    private val store = sessionStore ?: TealAccountSessionStore(
        TealApiClient.json.decodeFromString<TealSession>(userAccount.authKey).id,
        read = { PlatformStuff.mainPrefs.data.first().scrobbleAccounts },
        update = { transform ->
            PlatformStuff.mainPrefs.updateData { prefs -> prefs.copy(scrobbleAccounts = transform(prefs.scrobbleAccounts)) }
        },
    )
    private val client by lazy {
        val coordinator = if (usePersistedAccounts) refreshCoordinator else TealRefreshCoordinator.processLocal
        TealApiClient({ request ->
            if (httpClient == null) TealTransport.withClient(request) else request(httpClient)
        }, store, coordinator)
    }
    private val pages = mutableMapOf<Triple<Long, Long, Int>, MutableMap<Int, String?>>()
    private val pageLock = Mutex()

    private inner class PlayEnrichment {
        private var loadedAccount = false
        private var account: UserAccountSerializable? = null
        private var lookupBatch: ListenBrainzMbidResolver.Batch? = null

        suspend fun enrich(data: ScrobbleData): ScrobbleData {
            if (!usePersistedAccounts || mbidUri(data.extraTags[ListenBrainz.MUSICBRAINZ_TRACKID]) != null) return data
            if (!loadedAccount) {
                lookupBatchStartupLock.withLock {
                    account = configuredLookupAccount()
                    lookupBatch = mbidResolver.startBatch(account)
                    loadedAccount = true
                }
            }
            val lookupArtist = data.extraTags[FULL_ARTIST_CREDIT_TAG] ?: data.artist
            val match = lookupBatch?.lookup?.invoke(lookupArtist, data.track, data.album) ?: return data
            val credits = match.metadata?.artist?.artists.orEmpty()
            val creditsMatch = credits.size > 1 &&
                lookupArtist.trim().lowercase().split(artistCreditSeparator).sorted() ==
                credits.map { it.name.trim().lowercase() }.sorted()
            if (mbidUri(match.recording_mbid) == null || !match.recording_name.matchesLookupName(data.track) ||
                (!match.artist_credit_name.matchesLookupName(lookupArtist) && !creditsMatch)) return data
            val length = match.metadata?.recording?.length
            val duration = data.duration
            if (length != null && length > 0 && duration != null && duration > 0 && abs(length - duration) > 10_000)
                return data
            // A slow result from an old login must not enrich a play after account replacement.
            val current = configuredLookupAccount()
            if (current == null || current.authKey != account?.authKey || current.user.name != account?.user?.name) return data

            val tags = data.extraTags.toMutableMap()
            fun add(key: String, value: String?) {
                if (value != null && mbidUri(value) != null && mbidUri(tags[key]) == null) tags[key] = value
            }
            add(ListenBrainz.MUSICBRAINZ_TRACKID, match.recording_mbid)
            if (data.album?.isNotBlank() == true && match.release_name.matchesLookupName(data.album))
                add(ListenBrainz.MUSICBRAINZ_ALBUMID, match.release_mbid)
            val artistId = credits.singleOrNull { it.name.matchesLookupName(data.artist) }?.artist_mbid
                ?: match.artist_mbids?.singleOrNull()?.takeIf { match.artist_credit_name.matchesLookupName(data.artist) }
            add(ListenBrainz.MUSICBRAINZ_ARTISTID, artistId)
            return data.copy(extraTags = tags)
        }
    }

    private suspend fun writePlay(data: ScrobbleData, enrichment: PlayEnrichment): String {
        val key = data.tealRecordKey() // Validate original input before any optional lookup.
        val record = enrichment.enrich(data).toTealRecord(agent)
        return client.write(TEAL_PLAY, key, record, createOnly = true).string("uri")
    }

    override suspend fun scrobble(scrobbleData: ScrobbleData): Result<ScrobbleResult> = result {
        ScrobbleResult(false, writePlay(scrobbleData, PlayEnrichment()))
    }

    override suspend fun scrobble(scrobbleDatas: List<ScrobbleData>): Result<ScrobbleResult> = result {
        val enrichment = PlayEnrichment()
        var firstError: Exception? = null
        for (data in scrobbleDatas) {
            try {
                writePlay(data, enrichment)
            } catch (e: Exception) {
                if (e !is TealInvalidListenException && e !is TealRecordCollisionException) throw e
                if (firstError == null) firstError = e
            }
        }
        firstError?.let { throw it }
        ScrobbleResult(false)
    }

    override suspend fun updateNowPlaying(scrobbleData: ScrobbleData): Result<ScrobbleResult> = result {
        scrobbleData.validateTealFields()
        // Leave time to publish before the playback queue's Now Playing deadline.
        val enriched = withTimeoutOrNull(2.seconds) { PlayEnrichment().enrich(scrobbleData) }
        val item = (enriched ?: scrobbleData).toTealRecord(agent)
        val now = System.currentTimeMillis()
        client.write(TEAL_STATUS, "self", buildJsonObject {
            put("\$type", TEAL_STATUS)
            put("time", Instant.fromEpochMilliseconds(now).toString())
            put("expiry", Instant.fromEpochMilliseconds(now + (scrobbleData.duration ?: 600_000L).coerceIn(30_000L, 600_000L)).toString())
            put("item", JsonObject(item.filterKeys { it != "\$type" }))
        })
        ScrobbleResult(false)
    }

    override suspend fun getRecents(page: Int, username: String, cached: Boolean, from: Long,
        to: Long, includeNowPlaying: Boolean, limit: Int): Result<PageResult<Track>> = result {
        if (cached) return@result createEmptyPageResult<Track>()
        pageLock.withLock {
            require(page > 0 && limit > 0)
            val session = store.load()
            val pageSize = limit.coerceAtMost(100)
            val key = Triple(from, to, pageSize)
            val cursors = pages.getOrPut(key) { mutableMapOf(1 to null) }
            if (page == 1) { cursors.clear(); cursors[1] = null }
            var tracks = emptyList<Track>()
            var more = false
            // Reconstruct missing cursors when paging is recreated or refresh updates the account.
            for (currentPage in 1..page) {
                if (currentPage < page && cursors.containsKey(currentPage + 1)) continue
                var cursor = cursors[currentPage]
                val collected = mutableListOf<Track>()
                var requests = 0
                do {
                    val response = client.listRecords(session.pds, session.did, TEAL_PLAY,
                        pageSize - collected.size, cursor)
                    val records = response["records"]?.jsonArray ?: throw IOException("PDS response is missing records.")
                    collected += records.mapNotNull { record ->
                        val obj = record.jsonObject
                        toTrack(obj["value"]?.jsonObject ?: return@mapNotNull null, obj.string("uri"))
                    }.filter { (from < 0 || (it.date ?: 0) >= from) && (to < 0 || (it.date ?: 0) <= to) }
                    val next = response["cursor"]?.jsonPrimitive?.contentOrNull
                    check(next == null || next != cursor) { "PDS returned a repeated cursor." }
                    cursor = next
                    requests++
                    if (requests >= 100 && collected.size < pageSize && cursor != null)
                        throw IOException("Too many records for this date filter. Try a more recent date.")
                } while (cursor != null && collected.size < pageSize)
                tracks = collected.sortedByDescending { it.date }
                more = cursor != null
                if (more) cursors[currentPage + 1] = cursor
                else if (currentPage < page) { tracks = emptyList(); break }
            }
            if (includeNowPlaying && page == 1) {
                try {
                    val status = client.getRecord(session.pds, session.did, TEAL_STATUS, "self")["value"]!!.jsonObject
                    val time = Instant.parse(status.string("time")).toEpochMilliseconds()
                    val expiry = if ("expiry" in status) Instant.parse(status.string("expiry")).toEpochMilliseconds()
                    else time + 600_000
                    if (expiry > System.currentTimeMillis()) {
                        val item = status["item"]!!.jsonObject
                        val name = item["trackName"] as? JsonPrimitive
                        require(name?.isString == true && name.content.isNotBlank())
                        require(item.hasValidTealOptionalTrackFields())
                        item["playedTime"]?.let {
                            require(it is JsonPrimitive && it.isString && Instant.parseOrNull(it.content) != null)
                        }
                        item["artists"]?.let { artists ->
                            require(artists is JsonArray && artists.all {
                                val artist = (it as? JsonObject)?.get("artistName") as? JsonPrimitive
                                artist?.isString == true && artist.content.isNotBlank()
                            })
                        }
                        item["artistNames"]?.let { names ->
                            require(names is JsonArray && names.all {
                                it is JsonPrimitive && it.isString && it.content.isNotBlank()
                            })
                        }
                        toTrack(item, null)?.copy(date = null, isNowPlaying = true)?.let { tracks = listOf(it) + tracks }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (e !is TealApiException || e.error != "RecordNotFound")
                        Logger.w { "teal.fm optional status unavailable or invalid" }
                }
            }
            PageResult(PageAttr(page, if (more) page + 1 else page, null), tracks)
        }
    }

    // teal's play/status lexicons do not expose these Last.fm features.
    override suspend fun loveOrUnlove(track: Track, love: Boolean) = Result.success(ScrobbleResult(true))
    override suspend fun delete(track: Track): Result<Unit> = Result.failure(UnsupportedOperationException("teal.fm deletion is not supported."))
    override suspend fun getLoves(page: Int, username: String, cacheStrategy: CacheStrategy, limit: Int) = Result.success(createEmptyPageResult<Track>())
    override suspend fun getFriends(page: Int, username: String, cached: Boolean, limit: Int) = Result.success(createEmptyPageResult<User>())
    override suspend fun loadDrawerData(username: String): Result<DrawerData>? = null
    override suspend fun getCharts(type: Int, timePeriod: TimePeriod, page: Int, username: String, cacheStrategy: CacheStrategy, limit: Int): Result<PageResult<out MusicEntry>> = Result.success(createEmptyPageResult<Track>())
    override suspend fun getListeningActivity(timePeriod: TimePeriod, user: UserCached?, cacheStrategy: CacheStrategy) = ListeningActivity()

    companion object {
        private val refreshCoordinator by lazy {
            TealRefreshCoordinator(File(PlatformStuff.filesDir, "teal-refresh.lock"))
        }
        private val lookupBatchStartupLock = Mutex()
        private val mbidResolver by lazy { ListenBrainzMbidResolver(TealTransport::withClient) }
        private val artistCreditSeparator = Regex("""\s*[,;、]\s*|\s+(?:&|x|and|feat\.?|ft\.?|featuring|with|/)\s+""")
        private val agent get() = "com.arn.scrobble/${BuildKonfig.VER_NAME}"

        suspend fun login(identifier: String, appPassword: String): Result<Unit> = result {
            val session = TealApiClient(TealTransport::withClient).login(identifier, appPassword)
            Scrobblables.add(UserAccountSerializable(
                type = AccountType.TEALFM,
                user = UserCached(session.handle, "https://atproto.at/viewer?uri=${session.did}/$TEAL_PLAY", session.handle, "", -1),
                authKey = TealApiClient.json.encodeToString(session),
                apiRoot = session.pds,
            ))
        }

        private suspend fun configuredLookupAccount(): UserAccountSerializable? = try {
            PlatformStuff.mainPrefs.data.first().scrobbleAccounts.firstOrNull(ListenBrainzMbidResolver::isOfficialAccount)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

        private fun String?.matchesLookupName(other: String) = this?.trim()?.lowercase() == other.trim().lowercase()

        private fun toTrack(record: JsonObject, uri: String?): Track? {
            val name = record["trackName"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
            val artists = record["artists"]?.jsonArray?.mapNotNull { it.jsonObject["artistName"]?.jsonPrimitive?.contentOrNull }
                ?: record["artistNames"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
            return Track(name = name, artist = Artist(artists.joinToString(", ")),
                album = record["releaseName"]?.jsonPrimitive?.contentOrNull?.let { Album(it) },
                duration = record["duration"]?.jsonPrimitive?.longOrNull?.times(1000),
                date = record["playedTime"]?.jsonPrimitive?.contentOrNull?.let { Instant.parseOrNull(it)?.toEpochMilliseconds() },
                url = uri?.let { "https://atproto.at/viewer?uri=$it" }, msid = uri)
        }

        private suspend fun <T> result(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
            try { Result.success(block()) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
        }
    }
}

private object TealTransport {
    private class Entry(val base: HttpClient, val client: HttpClient) {
        var inUse = 0
    }

    private val lock = Any()
    private var current: Entry? = null

    suspend fun <T> withClient(request: suspend (HttpClient) -> T): T {
        val entry = synchronized(lock) {
            val base = Requesters.baseKtorClient
            val old = current
            val selected = if (old?.base === base) old else {
                // config() inherits engine ownership in Ktor. An explicit engine only borrows it.
                val client = HttpClient(base.engine) {
                    followRedirects = false
                    install(HttpTimeout) { requestTimeoutMillis = 40_000L }
                    install(UserAgent) { agent = "${BuildKonfig.APP_NAME} ${BuildKonfig.VER_NAME}" }
                }
                Entry(base, client).also {
                    current = it
                    if (old?.inUse == 0) old.client.close()
                }
            }
            selected.also { it.inUse++ }
        }
        try {
            return request(entry.client)
        } finally {
            synchronized(lock) {
                if (--entry.inUse == 0 && entry !== current) entry.client.close()
            }
        }
    }
}
