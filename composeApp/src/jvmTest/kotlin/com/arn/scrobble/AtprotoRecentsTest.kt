package com.arn.scrobble

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.atproto.AT_RECENTS_MAX_BYTES
import com.arn.scrobble.api.atproto.AT_RECENTS_PATH
import com.arn.scrobble.api.atproto.AtprotoScrobblable
import com.arn.scrobble.api.atproto.atprotoRecentsClient
import com.arn.scrobble.api.atproto.playCollection
import com.arn.scrobble.api.cache.CacheStrategy
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.utils.Stuff.cacheStrategy
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AtprotoRecentsTest {
    @Test
    fun readsTwoFullPagesAndTailForEachSourceWithoutCredentials() = bothSources { type ->
        fixture(type, { q ->
            val start = q["cursor"]?.substringAfter("+/")?.toInt() ?: 0
            val end = minOf(start + q.getValue("limit").toInt(), 237)
            records((start until end).map { row(type, it) }, ("+/" + end).takeIf { end < 237 })
        }) { scrobbler, requests ->
            assertTrue(scrobbler.getRecents(1, cached = true).isFailure)
            assertTrue(requests.isEmpty())
            val pages = (1..3).map { scrobbler.getRecents(it, limit = 100, includeNowPlaying = true).getOrThrow() }
            assertEquals(listOf(100, 100, 37), pages.map { it.entries.size })
            assertEquals(listOf(2, 3, 3), pages.map { it.attr.totalPages })
            assertTrue(pages.all { it.attr.total == null })
            assertEquals((0..236).map { "Track $it" }, pages.flatMap { it.entries }.map { it.name })
            val first = pages.first().entries.first()
            assertEquals(125_000L, first.duration)
            assertEquals(startMillis, first.date)
            assertEquals("First, Second", first.artist.name)
            assertEquals("Release", first.album?.name)
            assertFalse(first.isNowPlaying)
            assertEquals(listOf(null, "+/100", "+/200"), requests.map { it["cursor"] })
            assertTrue(scrobbler.getRecents(2, username = "other.example", limit = 100).isFailure)
            assertTrue(scrobbler.getRecents(2, limit = 99).isFailure)
            assertTrue(scrobbler.getRecents(2, limit = 100, to = startMillis).isFailure)
            assertEquals(3, requests.size)
            assertEquals(pages[1], scrobbler.getRecents(2, limit = 100).getOrThrow())
            assertEquals(4, requests.size)
            assertEquals("+/100", requests.last()["cursor"])
            assertEquals(pages[2], scrobbler.getRecents(3, limit = 100).getOrThrow())
            assertEquals(pages.first(), scrobbler.getRecents(1, limit = 100).getOrThrow())
            assertNull(requests.last()["cursor"])
            assertTrue(scrobbler.getRecents(3, limit = 100).isFailure)
        }
    }

    @Test
    fun walksPastFilteredAndMalformedRecordsUsingActualPlayTime() = bothSources { type ->
        fixture(type, { q ->
            when (q["cursor"]) {
                null -> records(listOf(row(type, 0), row(type, 7, time = "bad")), "filtered")
                "filtered" -> records(listOf(row(type, 5, legacy = true), row(type, 0)), "same-track")
                "same-track" -> records(listOf(row(type, 5, legacy = true)), "older-key")
                "older-key" -> records(listOf(row(type, 6)), "terminal")
                else -> records(emptyList())
            }
        }) { scrobbler, requests ->
            val from = startMillis - 6000
            val to = startMillis - 5000
            val first = scrobbler.getRecents(1, from = from, to = to, limit = 2).getOrThrow()
            assertEquals(listOf("Track 5", "Track 6"), first.entries.map { it.name })
            assertEquals("Legacy", first.entries.first().artist.name)
            assertEquals(to, first.entries.first().date)
            assertEquals(first, scrobbler.getRecents(1, from = from, to = to, limit = 2, cached = true).getOrThrow())
            assertEquals(4, requests.size)
            val last = scrobbler.getRecents(2, from = from, to = to, limit = 2).getOrThrow()
            assertEquals(emptyList(), last.entries)
            assertEquals(2, last.attr.totalPages)
            assertEquals(listOf(null, "filtered", "same-track", "older-key", "terminal"), requests.map { it["cursor"] })
        }
    }

    @Test
    fun previewsFromDiskAfterReopeningAndRefreshesFromNetwork() = runBlocking {
        val version = AtomicInteger(0)
        withPds(setOf(AccountType.TEAL, AccountType.ROCKSKY), { type, _ ->
            val current = version.get()
            if (current < 0) 503 to "{}" else records(listOf(row(type, current)))
        }) { pds, cacheDir, requests ->
            withRecentsClient(cacheDir) { client ->
                val teal = scrobbler(AccountType.TEAL, pds, client)
                assertEquals(504, assertIs<ApiException>(teal.getRecents(1, cached = true).exceptionOrNull()).code)
                assertTrue(requests.isEmpty())
                assertEquals("Track 0", teal.getRecents(1).getOrThrow().entries.single().name)
            }
            assertTrue(cacheDir.listFiles().orEmpty().any { it.isFile && it.length() > 0 })
            version.set(1)

            withRecentsClient(cacheDir) { client ->
                val teal = scrobbler(AccountType.TEAL, pds, client)
                val rocksky = scrobbler(AccountType.ROCKSKY, pds, client)
                assertEquals("Track 0", teal.getRecents(1, cached = true).getOrThrow().entries.single().name)
                assertTrue(rocksky.getRecents(1, cached = true).isFailure)
                assertTrue(teal.getRecents(1, cached = true, limit = 1).isFailure)
                assertEquals(1, requests.size)
                assertEquals("Track 1", rocksky.getRecents(1).getOrThrow().entries.single().name)
                version.set(2)
                assertEquals("Track 2", teal.getRecents(1).getOrThrow().entries.single().name)
                assertEquals(listOf(AccountType.TEAL, AccountType.ROCKSKY, AccountType.TEAL).map { it.playCollection },
                    requests.map { it["collection"] })
            }

            withRecentsClient(cacheDir) { client ->
                val teal = scrobbler(AccountType.TEAL, pds, client)
                assertEquals("Track 2", teal.getRecents(1, cached = true).getOrThrow().entries.single().name)
                assertEquals("Track 1", scrobbler(AccountType.ROCKSKY, pds, client)
                    .getRecents(1, cached = true).getOrThrow().entries.single().name)
                assertEquals(3, requests.size)
                version.set(-1)
                assertEquals(503, assertIs<ApiException>(teal.getRecents(1).exceptionOrNull()).code)
                assertEquals("Track 2", teal.getRecents(1, cached = true).getOrThrow().entries.single().name)
                assertEquals(4, requests.size)
            }
        }
    }

    @Test
    fun refreshFollowsNewCursorChainAndOfflineReadsStopAtFirstMissingPage() = bothSources { type ->
        val oldCursors = listOf(null, "q+7/Q=", "Rp0+W=")
        val newCursors = listOf(null, "Vf/4+=", "+euY=", "k/Z+0=")
        val shifted = AtomicBoolean(false)
        withPds(setOf(type), { _, q ->
            val fresh = shifted.get()
            val cursors = if (fresh) newCursors else oldCursors
            val page = cursors.indexOf(q["cursor"])
            if (page < 0) 400 to """{"error":"InvalidRequest","message":"Expired cursor"}"""
            else {
                val start = (if (fresh) 7 else 10) + page * 2
                records((start..minOf(start + 1, 14)).map { row(type, it) }, cursors.getOrNull(page + 1))
            }
        }) { pds, cacheDir, requests ->
            suspend fun offline(vararg expectedPages: IntRange, nextPageMissing: Boolean = false) {
                val before = requests.size
                withRecentsClient(cacheDir, offline = true) { client ->
                    val reader = scrobbler(type, pds, client)
                    expectedPages.forEachIndexed { i, ids ->
                        assertEquals(ids.map { "Track $it" }, reader.pageNames(i + 1, cached = true))
                    }
                    if (nextPageMissing) {
                        val miss = reader.getRecents(expectedPages.size + 1, cached = true, limit = 2)
                        assertEquals(504, assertIs<ApiException>(miss.exceptionOrNull()).code)
                        assertIs<IOException>(reader.getRecents(1, limit = 2).exceptionOrNull())
                        // The old page-two body still exists, but the new page one no longer points to it.
                        assertEquals(records((12..13).map { row(type, it) }, oldCursors[2]).second,
                            client.get("$pds$AT_RECENTS_PATH") {
                                parameter("repo", did)
                                parameter("collection", type.playCollection)
                                parameter("limit", 2)
                                parameter("cursor", oldCursors[1])
                                cacheStrategy(CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED)
                            }.bodyAsText())
                    }
                }
                assertEquals(before, requests.size)
            }

            withRecentsClient(cacheDir) { client ->
                val reader = scrobbler(type, pds, client)
                assertEquals((10..14).map { "Track $it" }, (1..3).flatMap { reader.pageNames(it) })
                assertEquals(oldCursors, requests.map { it["cursor"] })
                offline(10..11, 12..13, 14..14)

                shifted.set(true)
                val first = reader.pageNames(1)
                offline(7..8, nextPageMissing = true)
                val second = reader.pageNames(2)
                offline(7..8, 9..10, nextPageMissing = true)
                assertEquals((7..14).map { "Track $it" }, first + second + reader.pageNames(3) + reader.pageNames(4))
                assertEquals(oldCursors + newCursors, requests.map { it["cursor"] })
                offline(7..8, 9..10, 11..12, 13..14)
            }
        }
    }

    @Test
    fun retryRepeatsRejectedCursorButRefreshRebuildsChain() = bothSources { type ->
        val invalidated = AtomicBoolean(false)
        val oldCursors = listOf(null, "u/Dr=", "n+P3=")
        val newCursors = listOf(null, "G+ne=", "Hz/I=")
        fixture(type, { q ->
            val cursors = if (invalidated.get()) newCursors else oldCursors
            val page = cursors.indexOf(q["cursor"])
            if (page < 0) 400 to """{"error":"InvalidRequest","message":"Expired cursor"}"""
            else records((page * 2..page * 2 + 1).map { row(type, it) }, cursors.getOrNull(page + 1))
        }) { reader, requests ->
            reader.pageNames(1)
            reader.pageNames(2)
            invalidated.set(true)

            // Paging retries the failed append key; it does not issue a page-one refresh.
            repeat(2) {
                val result = reader.getRecents(3, limit = 2)
                assertEquals(400, assertIs<ApiException>(result.exceptionOrNull()).code)
            }
            assertEquals(oldCursors + oldCursors[2], requests.map { it["cursor"] })

            assertEquals((0..5).map { "Track $it" }, (1..3).flatMap { reader.pageNames(it) })
            assertEquals(oldCursors + oldCursors[2] + newCursors, requests.map { it["cursor"] })
        }
    }

    @Test
    fun doesNotCachePrivateOrOversizedResponses() = runBlocking {
        val version = AtomicInteger(0)
        withPds(setOf(AccountType.TEAL), { type, _ ->
            when (version.get()) {
                2 -> 200 to " ".repeat(AT_RECENTS_MAX_BYTES.toInt() + 1)
                else -> records(listOf(row(type, version.get())))
            }
        }, cacheControl = {
            when (version.get()) {
                0 -> "PRIVATE=\"Set-Cookie\""
                1 -> "no-store"
                else -> null
            }
        }) { pds, cacheDir, requests ->
            withRecentsClient(cacheDir) { client ->
                assertFailsWith<IllegalStateException> { client.get("$pds/xrpc/com.atproto.server.getSession") }
                assertFailsWith<IllegalStateException> {
                    client.get("$pds$AT_RECENTS_PATH") {
                        parameter("repo", did)
                        parameter("collection", AccountType.TEAL.playCollection)
                        header(HttpHeaders.Authorization, "Bearer fixture")
                    }
                }
                assertTrue(requests.isEmpty())
                val teal = scrobbler(AccountType.TEAL, pds, client)
                repeat(3) { n ->
                    version.set(n)
                    val result = teal.getRecents(1)
                    if (n < 2) assertEquals("Track $n", result.getOrThrow().entries.single().name)
                    else assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("too large"))
                    assertEquals(504, assertIs<ApiException>(teal.getRecents(1, cached = true).exceptionOrNull()).code)
                    assertEquals(n + 1, requests.size)
                }
                assertFalse(cacheDir.listFiles().orEmpty().any { it.isFile })
            }
        }
    }

    @Test
    fun surfacesHttpErrorsAndRepeatedCursorsWithoutPoisoningRetry() = bothSources { type ->
        val failing = AtomicBoolean(true)
        fixture(type, { q ->
            when (q["cursor"]) {
                null -> records(listOf(row(type, 0), row(type, 1)), "next")
                "next" -> if (failing.getAndSet(false)) {
                    503 to """{"error":"Unavailable"}"""
                } else {
                    records(listOf(row(type, 2)), "cycle")
                }
                "cycle" -> records(listOf(row(type, 2)), "next")
                else -> error("Unexpected cursor")
            }
        }) { scrobbler, requests ->
            val first = scrobbler.getRecents(1, limit = 2).getOrThrow()
            assertEquals(503, assertIs<ApiException>(scrobbler.getRecents(2, limit = 2).exceptionOrNull()).code)
            assertTrue(scrobbler.getRecents(2, limit = 2).exceptionOrNull()?.message.orEmpty().contains("repeated"))
            assertEquals(first, scrobbler.getRecents(1, limit = 2, cached = true).getOrThrow())
            assertTrue(scrobbler.getRecents(2, limit = 2, cached = true).isFailure)
            assertEquals(listOf(null, "next", "next", "cycle"), requests.map { it["cursor"] })
        }
    }

    private fun bothSources(block: suspend (AccountType) -> Unit) = runBlocking {
        for (type in listOf(AccountType.TEAL, AccountType.ROCKSKY)) block(type)
    }

    private suspend fun fixture(
        type: AccountType,
        respond: (Map<String, String>) -> Pair<Int, String>,
        block: suspend (AtprotoScrobblable, Collection<Map<String, String>>) -> Unit,
    ) = withPds(setOf(type), { _, q -> respond(q) }) { pds, cacheDir, requests ->
        withRecentsClient(cacheDir) { client -> block(scrobbler(type, pds, client), requests) }
    }

    private fun scrobbler(type: AccountType, pds: String, client: HttpClient): AtprotoScrobblable {
        val user = UserCached("fixture.example", "https://fixture.example", "", "", 0)
        val account = UserAccountSerializable(type, user, did, pds)
        return AtprotoScrobblable(account) { client }
    }

    private suspend fun AtprotoScrobblable.pageNames(page: Int, cached: Boolean = false) =
        getRecents(page, cached = cached, limit = 2).getOrThrow().entries.map { it.name }

    private suspend fun withRecentsClient(cacheDir: File, offline: Boolean = false, block: suspend (HttpClient) -> Unit) {
        HttpClient(OkHttp) {
            if (offline) engine { config { addInterceptor { throw IOException("Fixture is offline") } } }
        }.use { base -> base.atprotoRecentsClient(cacheDir).use { block(it) } }
    }

    private suspend fun withPds(
        types: Set<AccountType>,
        respond: (AccountType, Map<String, String>) -> Pair<Int, String>,
        cacheControl: (() -> String?)? = null,
        block: suspend (String, File, Collection<Map<String, String>>) -> Unit,
    ) {
        val requests = ConcurrentLinkedQueue<Map<String, String>>()
        val badRequest = AtomicBoolean(false)
        val cacheDir = createTempDirectory("atproto-recents-").toFile()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext(AT_RECENTS_PATH) { exchange ->
            val q = exchange.requestURI.rawQuery.split('&').associate {
                val (key, value) = it.split('=', limit = 2)
                key to URLDecoder.decode(value, Charsets.UTF_8)
            }
            requests += q
            val type = types.singleOrNull { q["collection"] == it.playCollection }
            if (exchange.requestMethod != "GET" || q["repo"] != did || type == null ||
                listOf("Authorization", "Cookie").any { exchange.requestHeaders.containsKey(it) }) {
                badRequest.set(true)
                exchange.respond(400 to "{}")
            } else {
                cacheControl?.invoke()?.let { exchange.responseHeaders.set("Cache-Control", it) }
                exchange.respond(respond(type, q))
            }
        }
        server.start()
        try {
            block("http://127.0.0.1:${server.address.port}", cacheDir, requests)
            assertFalse(badRequest.get())
        } finally {
            server.stop(0)
            cacheDir.deleteRecursively()
        }
    }

    private fun HttpExchange.respond(response: Pair<Int, String>) {
        val body = response.second.encodeToByteArray()
        responseHeaders.set("Content-Type", "application/json")
        sendResponseHeaders(response.first, body.size.toLong())
        responseBody.use { it.write(body) }
    }

    private fun records(rows: List<String>, cursor: String? = null) = 200 to
        """{"records":[${rows.joinToString(",")}]${cursor?.let { ",\"cursor\":\"$it\"" }.orEmpty()}}"""

    private fun row(type: AccountType, i: Int, legacy: Boolean = false, time: String = Instant.ofEpochMilli(startMillis - i * 1000).toString()): String {
        val teal = type == AccountType.TEAL
        val artist = when {
            teal && legacy -> """"artistNames":["Legacy"]"""
            teal -> """"artists":[{"artistName":"First"},{"artistName":"Second"}]"""
            legacy -> "\"artist\":\"Legacy\""
            else -> """"artists":[{"name":"First"},{"name":"Second"}]"""
        }
        val track = if (teal)
            """"trackName":"Track $i","releaseName":"Release","playedTime":"$time","duration":125"""
        else
            """"title":"Track $i","album":"Release","createdAt":"$time","duration":125000"""
        return """{"uri":"at://$did/${type.playCollection}/key-$i","value":{"${'$'}type":"${type.playCollection}",$artist,$track}}"""
    }

    private val did = "did:plc:abcdefghijklmnopqrstuvwx"
    private val startMillis = Instant.parse("2025-04-01T00:00:00Z").toEpochMilli()
}
