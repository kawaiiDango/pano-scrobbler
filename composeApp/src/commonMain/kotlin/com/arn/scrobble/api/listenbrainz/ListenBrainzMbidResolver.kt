package com.arn.scrobble.api.listenbrainz

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.utils.Stuff
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fromHttpToGmtDate
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

internal class ListenBrainzMbidResolver(
    private val withHttp: suspend (suspend (HttpClient) -> ListenBrainzMbidLookup?) -> ListenBrainzMbidLookup?,
) {
    private data class LookupKey(val artist: String, val recording: String, val release: String?)
    private data class CachedLookup(val match: ListenBrainzMbidLookup?, val until: Duration)
    private class Session(val fingerprint: String) {
        var authCooldownUntil = Duration.ZERO
    }

    private class LookupHttpFailure(val status: Int, val retryAfter: String?) : IOException()

    private val mutex = Mutex()
    private val clock = TimeSource.Monotonic.markNow()
    private val cache = LinkedHashMap<LookupKey, CachedLookup>(16, 0.75f, true)
    private val inFlight =
        mutableMapOf<Pair<Session, LookupKey>, Deferred<ListenBrainzMbidLookup?>>()
    private var activeSession: Session? = null
    private var cooldownUntil = Duration.ZERO

    suspend fun startBatch(account: UserAccountSerializable?): Batch? {
        val official = account?.takeIf(::isOfficialAccount)
        val fingerprint = official?.let {
            MessageDigest.getInstance("SHA-256")
                .digest("${it.user.name}\u0000${it.authKey}".encodeToByteArray()).toHexString()
        }
        val session = mutex.withLock {
            if (activeSession?.fingerprint != fingerprint) {
                activeSession = fingerprint?.let(::Session)
                cache.clear()
            }
            activeSession
        } ?: return null
        val token = checkNotNull(official).authKey
        val batchLookups = mutableMapOf<LookupKey, ListenBrainzMbidLookup>()
        return Batch { artist, recording, release ->
            val query = LookupKey(
                artist.trim(),
                recording.trim(),
                release?.trim()?.takeIf { it.isNotEmpty() })
            val key = LookupKey(
                query.artist.lowercase(),
                query.recording.lowercase(),
                query.release?.lowercase()
            )
            if (query.artist.isEmpty() || query.recording.isEmpty()) null
            else batchLookups[key] ?: lookup(session, key, query, token)?.also {
                batchLookups[key] = it
            }
        }
    }

    // Each batch calls this sequentially; concurrent batches share the process cache and request jobs.
    class Batch internal constructor(val lookup: suspend (String, String, String?) -> ListenBrainzMbidLookup?)

    private suspend fun lookup(
        session: Session,
        key: LookupKey,
        query: LookupKey,
        token: String
    ): ListenBrainzMbidLookup? {
        val flightKey = session to key
        val pending = mutex.withLock {
            if (activeSession !== session) return null
            val now = clock.elapsedNow()
            cache[key]?.let {
                if (now < it.until) return it.match
                cache.remove(key)
            }
            inFlight[flightKey]?.let { return@withLock it }
            if (now < cooldownUntil || now < session.authCooldownUntil || inFlight.size >= 4) return null
            // A cancelled waiter must not cancel another batch's lookup. Start I/O after releasing the mutex.
            Stuff.appScope.async(Dispatchers.IO, start = CoroutineStart.LAZY) {
                try {
                    fetch(session, key, query, token)
                } finally {
                    withContext(NonCancellable) { mutex.withLock { inFlight.remove(flightKey) } }
                }
            }.also { inFlight[flightKey] = it }
        }
        pending.start()
        return pending.await()
    }

    private suspend fun fetch(
        session: Session,
        key: LookupKey,
        query: LookupKey,
        token: String
    ): ListenBrainzMbidLookup? {
        try {
            val result = withTimeoutOrNull(3.seconds) {
                val match = request(query, token)
                val ttl = if (match == null) 10.minutes else 24.hours
                CachedLookup(match, clock.elapsedNow() + ttl)
            } ?: throw IOException("ListenBrainz lookup timed out.")
            mutex.withLock {
                if (activeSession === session) {
                    cache[key] = result
                    if (cache.size > 256) cache.remove(cache.keys.first())
                }
            }
            return result.match
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mutex.withLock {
                val now = clock.elapsedNow()
                val failure = e as? LookupHttpFailure
                when (failure?.status) {
                    401, 403 -> session.authCooldownUntil = now + 30.seconds
                    429 -> cooldownUntil =
                        maxOf(cooldownUntil, now + retryDelay(failure.retryAfter))

                    null, in 500..599 -> cooldownUntil = maxOf(cooldownUntil, now + 10.seconds)
                    else -> Unit
                }
            }
            return null
        }
    }

    private suspend fun request(query: LookupKey, token: String): ListenBrainzMbidLookup? =
        withHttp { http ->
            val response = http.get("${Stuff.LISTENBRAINZ_API_ROOT}1/metadata/lookup/") {
                headers.append(HttpHeaders.Authorization, "Token $token")
                url {
                    parameters.append("artist_name", query.artist)
                    parameters.append("recording_name", query.recording)
                    query.release?.let { parameters.append("release_name", it) }
                    parameters.append("metadata", "true")
                    parameters.append("inc", "artist")
                }
            }
            if (response.status != HttpStatusCode.OK)
                throw LookupHttpFailure(
                    response.status.value,
                    response.headers[HttpHeaders.RetryAfter]
                )
            val payload = json.parseToJsonElement(response.bodyAsText()).jsonObject
            // ListenBrainz returns an empty object for a genuine no-match, not for a service error.
            if (payload.isEmpty()) null else
                json.decodeFromJsonElement<ListenBrainzMbidLookup>(payload)
                    .also {
                        if (it.recording_mbid.isNullOrBlank() || it.recording_name.isNullOrBlank() || it.artist_credit_name.isNullOrBlank())
                            throw IOException("ListenBrainz returned an invalid lookup result.")
                    }
        }

    private fun retryDelay(header: String?): Duration {
        val value = header?.trim() ?: return 30.seconds
        value.toLongOrNull()?.takeIf { it >= 0 }?.let { return it.seconds }
        val millis =
            runCatching { value.fromHttpToGmtDate().timestamp - System.currentTimeMillis() }.getOrNull()
        return millis?.coerceAtLeast(0)?.milliseconds ?: 30.seconds
    }

    companion object {
        private val json = Stuff.myJson

        fun isOfficialAccount(account: UserAccountSerializable) =
            account.type == AccountType.LISTENBRAINZ &&
                    account.apiRoot == Stuff.LISTENBRAINZ_API_ROOT && account.authKey.isNotBlank()
    }
}
