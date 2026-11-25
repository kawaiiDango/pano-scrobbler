package com.arn.scrobble.api.teal

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.ScrobbleData
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TealTest {
    private val did = "did:plc:abcdefghijklmnopqrstuvwx"
    private val pds = "https://pds.example.com"
    private val appPassword = "abcd-efgh-ijkl-mnop"
    private val track = ScrobbleData("Artist", "Song", "Album", 1_730_000_000_000,
        albumArtist = null, duration = 180_000, appId = "player")
    private fun session() = TealSession(did, "alice.example.com", pds, "old-access", "old-refresh")
    private class Store(@Volatile var value: TealSession) : TealSessionStore {
        val saves = AtomicInteger()
        override suspend fun load() = value
        override suspend fun save(session: TealSession) {
            value = session
            saves.incrementAndGet()
        }
    }
    private fun tokens(access: String = "new-access", refresh: String = "new-refresh") =
        """{"did":"$did","handle":"alice.example.com","accessJwt":"$access","refreshJwt":"$refresh"}"""
    private val written = """{"uri":"at://$did/$TEAL_PLAY/key","cid":"cid"}"""

    private fun assertFailurePropagated(expected: IOException, actual: IOException) {
        // Coroutine stack-trace recovery can wrap an exception in a copy of the same type.
        assertTrue(generateSequence<Throwable>(actual) { it.cause }.any { it === expected })
    }

    private fun playRecordJson(
        name: String = "Song",
        playedTime: String = "2024-10-27T03:33:20Z",
        agent: String = "pano/1",
    ) = """
        {
          "${'$'}type":"fm.teal.feed.play",
          "trackName":"$name",
          "artists":[{"artistName":"Artist"}],
          "releaseName":"Album",
          "duration":180,
          "playedTime":"$playedTime",
          "submissionClientAgent":"$agent"
        }
    """.trimIndent()

    private fun HttpRequestData.assertGetRecord(collection: String, key: String) {
        assertEquals(HttpMethod.Get, method)
        assertEquals("/xrpc/com.atproto.repo.getRecord", url.encodedPath)
        assertEquals("pds.example.com", url.host)
        assertEquals(did, url.parameters["repo"])
        assertEquals(collection, url.parameters["collection"])
        assertEquals(key, url.parameters["rkey"])
    }

    private fun didDocument(handle: String) = """
        {
          "id": "$did",
          "alsoKnownAs": ["at://$handle"],
          "service": [{"id":"#atproto_pds","type":"AtprotoPersonalDataServer","serviceEndpoint":"$pds"}]
        }
    """.trimIndent()

    private fun HttpRequestData.assertPublicLookup(path: String, parameters: Set<String> = emptySet()) {
        assertEquals(HttpMethod.Get, method)
        assertEquals(path, url.encodedPath)
        assertEquals(parameters, url.parameters.names())
        assertNull(headers["Authorization"])
        assertFalse(headers.entries().any { (_, values) -> values.any { appPassword in it } })
        assertIs<OutgoingContent.NoContent>(body)
    }

    @Test fun loginResolvesPdsAndSendsPasswordOnlyThere() = runBlocking<Unit> {
        var credentialsSent = false
        val http = HttpClient(MockEngine { req ->
            when (req.url.host) {
                "public.api.bsky.app" -> {
                    req.assertPublicLookup("/xrpc/com.atproto.identity.resolveHandle", setOf("handle"))
                    assertEquals("alice.example.com", req.url.parameters["handle"])
                    respond("""{"did":"$did"}""")
                }
                "plc.directory" -> {
                    req.assertPublicLookup("/$did")
                    respond(didDocument("alice.example.com"))
                }
                "pds.example.com" -> {
                    assertEquals(HttpMethod.Post, req.method)
                    assertEquals("/xrpc/com.atproto.server.createSession", req.url.encodedPath)
                    val body = Json.parseToJsonElement((req.body as TextContent).text).jsonObject
                    assertEquals(did, body.string("identifier"))
                    assertEquals(appPassword, body.string("password"))
                    credentialsSent = true
                    respond(tokens())
                }
                else -> error("Unexpected destination")
            }
        }) { followRedirects = false }
        http.use {
            val result = TealApiClient(it).login("@alice.example.com", appPassword)
            assertTrue(credentialsSent)
            assertEquals(did, result.did)
            assertEquals(pds, result.pds)
            assertFalse(TealApiClient.json.encodeToString(result).contains("abcd-efgh"))
            assertFalse(result.toString().contains("new-access"))
        }
    }

    @Test fun handleMismatchStopsBeforeSendingPassword() = runBlocking<Unit> {
        val http = HttpClient(MockEngine { req ->
            when (req.url.host) {
                "public.api.bsky.app" -> respond("""{"did":"$did"}""")
                "plc.directory" -> respond(didDocument("somebody.example.com"))
                else -> error("Credentials must not be sent")
            }
        })
        http.use { assertFailsWith<IllegalArgumentException> { TealApiClient(it).login("alice.example.com", "app-password") } }
    }

    @Test fun concurrentWritesRefreshOnceAndPersistRotatedTokens() = runBlocking<Unit> {
        val store = Store(session())
        val writeCount = 8
        val expiredWrites = AtomicInteger()
        val allExpiredWritesStarted = CompletableDeferred<Unit>()
        val refreshes = AtomicInteger()
        val writes = AtomicInteger()
        val http = HttpClient(MockEngine { req ->
            if (req.url.encodedPath.endsWith("refreshSession")) {
                refreshes.incrementAndGet()
                assertEquals("Bearer old-refresh", req.headers["Authorization"])
                respond(tokens())
            } else if (req.headers["Authorization"] == "Bearer old-access") {
                if (expiredWrites.incrementAndGet() == writeCount) allExpiredWritesStarted.complete(Unit)
                allExpiredWritesStarted.await()
                respond("""{"error":"ExpiredToken"}""", HttpStatusCode.BadRequest)
            } else {
                assertEquals(1, store.saves.get())
                assertEquals("new-refresh", store.value.refreshJwt)
                assertEquals("Bearer new-access", req.headers["Authorization"])
                writes.incrementAndGet()
                respond(written)
            }
        })
        http.use {
            // A preference update can create another adapter: both must share the session lock.
            val clients = listOf(TealApiClient(it, store), TealApiClient(it, store))
            withTimeout(5.seconds) {
                (0 until writeCount).map { i ->
                    async { clients[i % 2].write(TEAL_PLAY, "key$i", track.toTealRecord("pano/1")) }
                }.awaitAll()
            }
            assertEquals(writeCount, expiredWrites.get())
            assertEquals(1, refreshes.get())
            assertEquals(writeCount, writes.get())
        }
    }

    @Test fun revokedSessionDoesNotLoopOrExposeServerMessage() = runBlocking<Unit> {
        var calls = 0
        val http = HttpClient(MockEngine {
            calls++
            respond("""{"error":"InvalidToken","message":"secret-password"}""", HttpStatusCode.Unauthorized)
        })
        http.use {
            val error = assertFailsWith<TealApiException> { TealApiClient(it, Store(session())).write(TEAL_PLAY, "key", track.toTealRecord("pano/1")) }
            assertEquals(1, calls)
            assertTrue(error.message!!.contains("Reconnect"))
            assertFalse(error.message!!.contains("secret-password"))
        }
    }

    @Test fun failedSessionSaveDoesNotUseUnpersistedTokens() = runBlocking<Unit> {
        val calls = mutableListOf<Pair<String, String?>>()
        val saveFailure = IOException("Storage unavailable")
        val store = object : TealSessionStore {
            override suspend fun load() = session()
            override suspend fun save(session: TealSession) {
                assertEquals("new-access", session.accessJwt)
                assertEquals("new-refresh", session.refreshJwt)
                throw saveFailure
            }
        }
        val http = HttpClient(MockEngine { req ->
            val path = req.url.encodedPath
            calls += path to req.headers["Authorization"]
            when (path) {
                "/xrpc/com.atproto.repo.putRecord" -> respond("""{"error":"ExpiredToken"}""", HttpStatusCode.BadRequest)
                "/xrpc/com.atproto.server.refreshSession" -> respond(tokens())
                else -> error("Unexpected request $path")
            }
        })
        http.use {
            val error = assertFailsWith<IOException> {
                TealApiClient(it, store).write(TEAL_PLAY, "key", track.toTealRecord("pano/1"))
            }
            assertFailurePropagated(saveFailure, error)
            assertEquals(listOf<Pair<String, String?>>(
                "/xrpc/com.atproto.repo.putRecord" to "Bearer old-access",
                "/xrpc/com.atproto.server.refreshSession" to "Bearer old-refresh",
            ), calls)
        }
    }

    @Test fun retryOfSameListenReturnsExistingRecord() = runBlocking<Unit> {
        val key = "3l7cfnrryyy22"
        val uri = "at://$did/$TEAL_PLAY/$key"
        val original = Json.parseToJsonElement(playRecordJson()).jsonObject
        val retry = Json.parseToJsonElement(playRecordJson(agent = "pano/2")).jsonObject
        val lostResponse = IOException("Response lost after commit")
        val calls = mutableListOf<String>()
        val http = HttpClient(MockEngine { req ->
            val path = req.url.encodedPath
            calls += path
            when (path) {
                "/xrpc/com.atproto.repo.putRecord" -> {
                    assertEquals(HttpMethod.Post, req.method)
                    val body = Json.parseToJsonElement((req.body as TextContent).text).jsonObject
                    assertEquals(did, body["repo"]?.jsonPrimitive?.content)
                    assertEquals(TEAL_PLAY, body["collection"]?.jsonPrimitive?.content)
                    assertEquals(key, body["rkey"]?.jsonPrimitive?.content)
                    assertEquals(JsonNull, body["swapRecord"])
                    if (calls.size == 1) {
                        assertEquals(original, body["record"])
                        throw lostResponse
                    }
                    assertEquals(retry, body["record"])
                    respond("""{"error":"InvalidSwap"}""", HttpStatusCode.BadRequest)
                }
                "/xrpc/com.atproto.repo.getRecord" -> {
                    req.assertGetRecord(TEAL_PLAY, key)
                    respond("""{"uri":"$uri","value":${playRecordJson()}}""")
                }
                else -> error("Unexpected request $path")
            }
        })
        http.use {
            val client = TealApiClient(it, Store(session()))
            val error = assertFailsWith<IOException> { client.write(TEAL_PLAY, key, original, true) }
            assertFailurePropagated(lostResponse, error)
            assertEquals(uri, client.write(TEAL_PLAY, key, retry, true)["uri"]?.jsonPrimitive?.content)
            assertEquals(listOf(
                "/xrpc/com.atproto.repo.putRecord",
                "/xrpc/com.atproto.repo.putRecord",
                "/xrpc/com.atproto.repo.getRecord",
            ), calls)
        }
    }

    @Test fun recordCollisionDoesNotOverwriteAnotherListen() = runBlocking<Unit> {
        val calls = mutableListOf<String>()
        val http = HttpClient(MockEngine { req ->
            val path = req.url.encodedPath
            calls += path
            when (path) {
                "/xrpc/com.atproto.repo.putRecord" -> {
                    val body = Json.parseToJsonElement((req.body as TextContent).text).jsonObject
                    assertEquals(JsonNull, body["swapRecord"])
                    respond("""{"error":"InvalidSwap"}""", HttpStatusCode.BadRequest)
                }
                "/xrpc/com.atproto.repo.getRecord" -> {
                    req.assertGetRecord(TEAL_PLAY, "key")
                    respond("""{"uri":"at://$did/$TEAL_PLAY/key","value":${playRecordJson(name = "Other song")}}""")
                }
                else -> error("Unexpected request $path")
            }
        })
        http.use {
            val listen = Json.parseToJsonElement(playRecordJson()).jsonObject
            assertFailsWith<TealRecordCollisionException> {
                TealApiClient(it, Store(session())).write(TEAL_PLAY, "key", listen, true)
            }
            assertEquals(listOf("/xrpc/com.atproto.repo.putRecord", "/xrpc/com.atproto.repo.getRecord"), calls)
        }
    }

    @Test fun mapsUnitsAndUsesStableTid() {
        val value = track.toTealRecord("pano/1")
        assertEquals(180, value["duration"]!!.jsonPrimitive.int)
        assertEquals("2024-10-27T03:33:20Z", value.string("playedTime"))
        assertEquals("Artist", value["artists"]!!.jsonArray.first().jsonObject.string("artistName"))
        val key = track.tealRecordKey()
        assertTrue(key.matches(Regex("[234567a-z]{13}")))
        assertEquals(key, track.copy().tealRecordKey())
        assertNotEquals(key, track.copy(timestamp = track.timestamp + 1).tealRecordKey())
    }

    private fun account(session: TealSession) = UserAccountSerializable(
        AccountType.TEALFM, UserCached(session.handle, "", session.handle, "", -1),
        TealApiClient.json.encodeToString(session), session.pds,
    )

    @Test fun refreshPreservesSettingsAndCannotResurrectLoggedOutOrReplacedSession() = runBlocking<Unit> {
        val original = session()
        var accounts = listOf(account(original))
        val store = TealAccountSessionStore(original.id, { accounts }, { accounts = it(accounts) })
        assertEquals(original, store.load())
        accounts = accounts.map { it.copy(canScrobble = false) }
        val rotated = original.copy(accessJwt = "rotated-access", refreshJwt = "rotated-refresh")
        store.save(rotated)
        assertFalse(accounts.single().canScrobble)
        assertEquals(rotated, store.load())
        val restored = TealAccountSessionStore(original.id, { accounts }, { accounts = it(accounts) })
        assertEquals(rotated, restored.load())
        accounts = emptyList()
        assertFailsWith<IOException> { store.save(rotated) }
        assertTrue(accounts.isEmpty())
        val replacement = session()
        accounts = listOf(account(replacement))
        assertFailsWith<IllegalStateException> { store.load() }
        assertFailsWith<IllegalStateException> { store.save(rotated) }
        assertEquals(replacement, TealApiClient.json.decodeFromString<TealSession>(accounts.single().authKey))
    }

    @Test fun recentsFollowOpaqueCursorsAndRespectDateFilters() = runBlocking<Unit> {
        val store = Store(session())
        val cursors = mutableListOf<String?>()
        val http = HttpClient(MockEngine { req ->
            assertEquals("/xrpc/com.atproto.repo.listRecords", req.url.encodedPath)
            val cursor = req.url.parameters["cursor"]
            cursors += cursor
            assertEquals(did, req.url.parameters["repo"])
            assertEquals(TEAL_PLAY, req.url.parameters["collection"])
            assertEquals("1", req.url.parameters["limit"])
            when (cursor) {
                null -> respond("""{"records":[{"uri":"at://$did/$TEAL_PLAY/key0","value":${playRecordJson("Song 0")}}],"cursor":"opaque-cursor"}""")
                "opaque-cursor" -> respond("""{"records":[{"uri":"at://$did/$TEAL_PLAY/key1","value":${playRecordJson("Song 1", "2024-10-27T03:33:19Z")}}]}""")
                else -> error("Unexpected cursor $cursor")
            }
        })
        http.use {
            val teal = Teal(account(store.value), store, it)
            val first = teal.getRecents(1, limit = 1).getOrThrow()
            assertEquals("Song 0", first.entries.single().name)
            assertEquals(2, first.attr.totalPages)
            val second = teal.getRecents(2, limit = 1).getOrThrow()
            assertEquals("Song 1", second.entries.single().name)
            assertEquals(2, second.attr.totalPages)
            assertEquals(listOf(null, "opaque-cursor"), cursors)
            val filtered = teal.getRecents(1, limit = 1, to = track.timestamp - 1).getOrThrow()
            assertEquals("Song 1", filtered.entries.single().name)
            assertEquals(listOf(null, "opaque-cursor", null, "opaque-cursor"), cursors)
        }
    }

    @Test fun recentsIncludeOnlyUnexpiredNowPlaying() = runBlocking<Unit> {
        val store = Store(session())
        val nowMillis = System.currentTimeMillis()
        val time = Instant.fromEpochMilliseconds(nowMillis)
        var expires = Instant.fromEpochMilliseconds(nowMillis + 60_000)
        val http = HttpClient(MockEngine { req ->
            when (req.url.encodedPath) {
                "/xrpc/com.atproto.repo.listRecords" -> respond("""{"records":[]}""")
                "/xrpc/com.atproto.repo.getRecord" -> {
                    req.assertGetRecord(TEAL_STATUS, "self")
                    respond("""
                        {"value":{"time":"$time","expiry":"$expires","item":{
                          "trackName":"Current song","artists":[{"artistName":"Current artist"}],"duration":180
                        }}}
                    """.trimIndent())
                }
                else -> error("Unexpected request ${req.url.encodedPath}")
            }
        })
        http.use {
            val teal = Teal(account(store.value), store, it)
            val now = teal.getRecents(1, includeNowPlaying = true).getOrThrow().entries.single()
            assertEquals("Current song", now.name)
            assertEquals("Current artist", now.artist.name)
            assertTrue(now.isNowPlaying)
            assertNull(now.date)
            expires = Instant.fromEpochMilliseconds(nowMillis - 1)
            assertTrue(teal.getRecents(1, includeNowPlaying = true).getOrThrow().entries.isEmpty())
        }
    }

    @Test fun validatesUtf8ByteLimitsForNonAsciiMetadata() {
        track.copy(track = "界".repeat(85)).toTealRecord("pano/1")
        assertFailsWith<IllegalArgumentException> { track.copy(track = "界".repeat(86)).toTealRecord("pano/1") }
        assertFailsWith<IllegalArgumentException> { track.copy(artist = "界".repeat(86)).toTealRecord("pano/1") }
        assertFailsWith<IllegalArgumentException> { track.copy(album = "界".repeat(86)).toTealRecord("pano/1") }
    }

    @Test fun refusesCredentialBearingOrInsecurePdsUrls() {
        listOf("http://pds.example.com", "https://user:pass@pds.example.com", "https://pds.example.com/path", "https://pds.example.com?secret=x")
            .forEach { assertFailsWith<IllegalArgumentException> { TealApiClient.httpsOrigin(it) } }
        assertEquals(pds, TealApiClient.httpsOrigin("$pds/"))
    }
}
