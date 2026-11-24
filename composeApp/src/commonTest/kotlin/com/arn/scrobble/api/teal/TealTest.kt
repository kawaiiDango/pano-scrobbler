package com.arn.scrobble.api.teal

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.ScrobbleData
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.IOException
import kotlin.test.*

class TealTest {
    private val did = "did:plc:abcdefghijklmnopqrstuvwx"
    private val pds = "https://pds.example.com"
    private val track = ScrobbleData("Artist", "Song", "Album", 1_730_000_000_000,
        albumArtist = null, duration = 180_000, appId = "player")
    private fun session() = TealSession(did, "alice.example.com", pds, "old-access", "old-refresh")
    private class Store(var value: TealSession) : TealSessionStore {
        var saves = 0
        override suspend fun load() = value
        override suspend fun save(session: TealSession) { value = session; saves++ }
    }
    private fun tokens(access: String = "new-access", refresh: String = "new-refresh") =
        """{"did":"$did","handle":"alice.example.com","accessJwt":"$access","refreshJwt":"$refresh"}"""
    private val written = """{"uri":"at://$did/$TEAL_PLAY/key","cid":"cid"}"""

    @Test fun loginResolvesPdsAndSendsPasswordOnlyThere() = runBlocking<Unit> {
        var credentialsSent = false
        val http = HttpClient(MockEngine { req ->
            when (req.url.host) {
                "public.api.bsky.app" -> {
                    assertNull(req.headers["Authorization"])
                    assertEquals("alice.example.com", req.url.parameters["handle"])
                    respond("""{"did":"$did"}""")
                }
                "plc.directory" -> respond("""{"id":"$did","alsoKnownAs":["at://alice.example.com"],"service":[{"id":"#atproto_pds","type":"AtprotoPersonalDataServer","serviceEndpoint":"$pds"}]}""")
                "pds.example.com" -> {
                    val body = Json.parseToJsonElement((req.body as TextContent).text).jsonObject
                    assertEquals(did, body.string("identifier"))
                    assertEquals("abcd-efgh-ijkl-mnop", body.string("password"))
                    credentialsSent = true
                    respond(tokens())
                }
                else -> error("Unexpected destination")
            }
        }) { followRedirects = false }
        http.use {
            val result = TealApiClient(it).login("@alice.example.com", "abcd-efgh-ijkl-mnop")
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
                "plc.directory" -> respond("""{"id":"$did","alsoKnownAs":["at://somebody.example.com"]}""")
                else -> error("Credentials must not be sent")
            }
        })
        http.use { assertFailsWith<IllegalArgumentException> { TealApiClient(it).login("alice.example.com", "app-password") } }
    }

    @Test fun concurrentWritesRefreshOnceAndPersistRotatedTokens() = runBlocking<Unit> {
        val store = Store(session())
        var refreshes = 0
        var writes = 0
        val http = HttpClient(MockEngine { req ->
            if (req.url.encodedPath.endsWith("refreshSession")) {
                refreshes++
                assertEquals("Bearer old-refresh", req.headers["Authorization"])
                respond(tokens())
            } else if (req.headers["Authorization"] == "Bearer old-access") {
                respond("""{"error":"ExpiredToken"}""", HttpStatusCode.BadRequest)
            } else {
                assertEquals(1, store.saves)
                assertEquals("new-refresh", store.value.refreshJwt)
                assertEquals("Bearer new-access", req.headers["Authorization"])
                writes++
                respond(written)
            }
        })
        http.use {
            // A preference update can create another adapter: both must share the session lock.
            val clients = listOf(TealApiClient(it, store), TealApiClient(it, store))
            coroutineScope {
                (0..7).map { i -> async { clients[i % 2].write(TEAL_PLAY, "key$i", track.toTealRecord("pano/1")) } }.awaitAll()
            }
            assertEquals(1, refreshes)
            assertEquals(8, writes)
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
        var calls = 0
        val store = object : TealSessionStore {
            override suspend fun load() = session()
            override suspend fun save(session: TealSession) { throw IOException("Storage unavailable") }
        }
        val http = HttpClient(MockEngine { req ->
            calls++
            if (req.url.encodedPath.endsWith("refreshSession")) respond(tokens())
            else respond("""{"error":"ExpiredToken"}""", HttpStatusCode.BadRequest)
        })
        http.use {
            assertFailsWith<IOException> { TealApiClient(it, store).write(TEAL_PLAY, "key", track.toTealRecord("pano/1")) }
            assertEquals(2, calls)
        }
    }

    @Test fun uncertainWriteRetryReconcilesExistingRecordEvenAfterUpgrade() = runBlocking<Unit> {
        val oldRecord = track.toTealRecord("pano/1")
        val newRecord = track.toTealRecord("pano/2")
        var saved: JsonObject? = null
        var writes = 0
        val http = HttpClient(MockEngine { req ->
            if (req.url.encodedPath.endsWith("getRecord")) {
                respond(buildJsonObject { put("uri", "at://$did/$TEAL_PLAY/key"); put("value", saved!!) }.toString())
            } else {
                writes++
                val body = Json.parseToJsonElement((req.body as TextContent).text).jsonObject
                assertEquals(JsonNull, body["swapRecord"])
                if (saved == null) {
                    saved = body["record"]!!.jsonObject
                    throw IOException("Response lost after commit")
                }
                respond("""{"error":"InvalidSwap"}""", HttpStatusCode.BadRequest)
            }
        })
        http.use {
            val client = TealApiClient(it, Store(session()))
            assertFailsWith<IOException> { client.write(TEAL_PLAY, track.tealRecordKey(), oldRecord, true) }
            client.write(TEAL_PLAY, track.tealRecordKey(), newRecord, true)
            assertEquals(2, writes)
            assertEquals(oldRecord, saved)
        }
    }

    @Test fun recordCollisionDoesNotOverwriteAnotherListen() = runBlocking<Unit> {
        val http = HttpClient(MockEngine { req ->
            if (req.url.encodedPath.endsWith("getRecord")) respond("""{"value":{"trackName":"Other song"}}""")
            else respond("""{"error":"InvalidSwap"}""", HttpStatusCode.BadRequest)
        })
        http.use {
            assertFailsWith<IOException> { TealApiClient(it, Store(session())).write(TEAL_PLAY, "key", track.toTealRecord("pano/1"), true) }
        }
    }

    @Test fun mapsUnitsAndUsesStableTid() {
        val value = track.toTealRecord("pano/1")
        assertEquals(180, value["duration"]!!.jsonPrimitive.int)
        assertEquals("2024-10-27T03:33:20Z", value.string("playedTime"))
        assertEquals("Artist", value["artists"]!!.jsonArray.first().jsonObject.string("artistName"))
        val key = track.tealRecordKey()
        assertEquals(13, key.length)
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
            val index = if (cursor == null) 0 else 1
            val value = track.copy(track = "Song $index", timestamp = track.timestamp - index * 1000).toTealRecord("pano/1")
            respond(buildJsonObject {
                putJsonArray("records") { add(buildJsonObject { put("uri", "at://$did/$TEAL_PLAY/key$index"); put("value", value) }) }
                if (cursor == null) put("cursor", "opaque-cursor")
            }.toString())
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
        }
    }

    @Test fun recentsIncludeOnlyUnexpiredNowPlaying() = runBlocking<Unit> {
        val store = Store(session())
        var expires = System.currentTimeMillis() + 60_000
        val http = HttpClient(MockEngine { req ->
            if (req.url.encodedPath.endsWith("listRecords")) respond("""{"records":[]}""")
            else respond(buildJsonObject {
                put("value", buildJsonObject {
                    put("time", kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString())
                    put("expiry", kotlin.time.Instant.fromEpochMilliseconds(expires).toString())
                    put("item", track.toTealRecord("pano/1"))
                })
            }.toString())
        })
        http.use {
            val teal = Teal(account(store.value), store, it)
            val now = teal.getRecents(1, includeNowPlaying = true).getOrThrow().entries.single()
            assertTrue(now.isNowPlaying)
            assertNull(now.date)
            expires = System.currentTimeMillis() - 1
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
