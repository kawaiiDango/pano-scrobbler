package com.arn.scrobble

import androidx.datastore.core.DataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.ScrobbleEvent
import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.atproto.AtprotoAppPasswordSession
import com.arn.scrobble.api.atproto.AtprotoScrobblable
import com.arn.scrobble.api.atproto.AtprotoSessions
import com.arn.scrobble.api.atproto.playCollection
import com.arn.scrobble.api.file.FileScrobblable
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.withoutScrobbleAccount
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.work.CommonWorkerResult
import com.arn.scrobble.work.PendingScrobblesWorker
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AtprotoDeliveryTest {
    @Test
    fun dispatchSendsBothProvidersAndFileBeforeReturningWithoutEnqueuing() = fixture { f ->
        f.connect()
        val accounts = f.adapters()
        f.database().use { db ->
            f.dispatch(db, play, accounts + f.fileAdapter)
            assertEquals(0, db.getPendingScrobblesDao().count())
        }
        assertTrue(f.scheduled.isEmpty())
        assertEquals(setOf(teal, rocksky), f.posts.map { it.collection }.toSet())
        assertEquals(2, f.posts.size)
        assertTrue(f.reads.isEmpty())
        assertEquals(play.timestamp * 1_000, decodeTidTime(f.posts.first().key))
        for (post in f.posts) {
            assertEquals(post.collection, post.record["\$type"]?.jsonPrimitive?.content)
            assertEquals("XXAAA2400001", post.record["isrc"]?.jsonPrimitive?.content)
            val isTeal = post.collection == teal
            assertEquals(play.track, post.record[if (isTeal) "trackName" else "title"]?.jsonPrimitive?.content)
            assertEquals(if (isTeal) "182" else "182320", post.record["duration"]?.jsonPrimitive?.content)
            assertEquals(post.record, f.records[post.uri])
        }
        assertTrue(f.csv.readText().contains(play.track))
    }

    @Test
    fun failedProviderAloneIsQueuedAndOrdinaryWorkerReconcilesLostReply() = fixture { f ->
        f.connect()
        f.failures[rocksky to play.track] = Failure.DropReply
        f.database().use { db ->
            f.dispatch(db, play, f.adapters() + f.fileAdapter)
            val pending = db.getPendingScrobblesDao().allScrobblesForSubmission(10).single()
            assertEquals(play, pending.scrobbleData)
            assertEquals(setOf(AccountType.ROCKSKY), pending.services)
        }
        assertEquals(listOf(false), f.scheduled)
        assertEquals(2, f.posts.size)
        assertTrue(f.reads.isEmpty())
        val recordsBefore = f.records.toMap()
        // Reopen Room and instantiate fresh adapters; nothing in RAM carries a key to the worker.
        f.database().use { db ->
            assertEquals(CommonWorkerResult.Success, f.work(db, f.adapters() + f.fileAdapter))
            assertEquals(0, db.getPendingScrobblesDao().count())
        }
        val rockskyAttempts = f.posts.filter { it.collection == rocksky }
        assertEquals(1, f.posts.count { it.collection == teal })
        assertEquals(2, rockskyAttempts.size)
        assertEquals(rockskyAttempts[0], rockskyAttempts[1])
        assertEquals(listOf(rockskyAttempts[0].uri), f.reads.toList())
        assertEquals(recordsBefore, f.records.toMap())
        assertEquals(1, f.csv.readLines().count { play.track in it })
    }

    @Test
    fun partialBatchRetriesTheCommittedPrefixWithoutOverwriting() = fixture { f ->
        f.connect()
        val second = play.copy(track = "Second", timestamp = play.timestamp + 300_000)
        val accounts = f.adapters()
        f.database().use { db ->
            for (item in listOf(play, second)) {
                f.failures[rocksky to item.track] = Failure.Unavailable
                f.dispatch(db, item, accounts)
            }
            val dao = db.getPendingScrobblesDao()
            assertEquals(2, dao.allScrobblesForSubmission(10).count { it.services == setOf(AccountType.ROCKSKY) })
            f.failures[rocksky to second.track] = Failure.DropReply
            assertEquals(CommonWorkerResult.Retry, f.work(db, accounts))
            assertEquals(2, dao.count())
        }
        assertTrue(f.reads.isEmpty())
        val stored = f.records.toMap()
        f.database().use { db ->
            assertEquals(CommonWorkerResult.Success, f.work(db, f.adapters()))
            assertEquals(0, db.getPendingScrobblesDao().count())
        }
        val replays = f.posts.filter { it.collection == rocksky }.groupBy { it.key }
        assertEquals(2, replays.size)
        assertTrue(replays.values.all { it.size == 3 && it.distinct().size == 1 })
        assertEquals(replays.values.map { it.first().uri }.toSet(), f.reads.toSet())
        assertEquals(4, stored.size)
        assertEquals(stored, f.records.toMap())
    }

    @Test
    fun differentRecordAtSameKeyIsNotAcknowledgedOrOverwritten() = fixture { f ->
        f.connect()
        val accounts = f.adapters()
        val unrelated = Json.parseToJsonElement("""{"trackName":"Different track"}""").jsonObject
        f.failures[teal to play.track] = Failure.Collision(unrelated)
        f.database().use { db ->
            f.dispatch(db, play, accounts)
            assertEquals(setOf(AccountType.TEAL), db.getPendingScrobblesDao().allScrobblesForSubmission(10).single().services)
        }
        val uri = f.posts.single { it.collection == teal }.uri
        assertEquals(listOf(uri), f.reads.toList())
        assertEquals(unrelated, f.records[uri])
    }

    @Test
    fun pausedAdapterDoesNotSendNewPlaysAndLoggedOutBitsAreCleanedNormally() = fixture { f ->
        f.connect()
        f.prefs.updateData { p -> p.copy(scrobbleAccounts = p.scrobbleAccounts.map {
            if (it.type == AccountType.ROCKSKY) it.copy(canScrobble = false) else it
        }) }
        f.database().use { db ->
            f.dispatch(db, play, f.adapters())
            assertEquals(teal, f.posts.single().collection)
            assertEquals(0, db.getPendingScrobblesDao().count())

            val dao = db.getPendingScrobblesDao()
            val backlog = play.copy(timestamp = play.timestamp - 5_000)
            dao.insert(backlog, ScrobbleEvent.scrobble, listOf(AccountType.TEAL, AccountType.ROCKSKY), emptyList())
            f.prefs.updateData { it.withoutScrobbleAccount(AccountType.TEAL) }
            assertEquals(CommonWorkerResult.Success, f.work(db, f.adapters()))
            // Like other providers, paused accounts still retry already-pending scrobbles.
            assertEquals(0, dao.count())
            assertEquals(rocksky, f.posts.last().collection)
            assertEquals(2, f.posts.size)
        }
    }

    @Test
    fun limitedActionsAreSuccessfulNoOpsWithoutPdsRequests() = fixture { f ->
        f.connect()
        for (adapter in f.adapters()) {
            assertTrue(adapter.updateNowPlaying(play).isSuccess)
            assertTrue(adapter.loveOrUnlove(play.toTrack(), true).isSuccess)
            assertTrue(adapter.loveOrUnlove(play.toTrack(), false).isSuccess)
        }
        assertTrue(f.posts.isEmpty())
        assertTrue(f.reads.isEmpty())
    }

    @Test
    fun sharedTokenRefreshAndTransportFailureDoNotLoseSavedLogin() = fixture { f ->
        f.connect("expired")
        val accounts = f.adapters()
        f.refreshStatus = 502
        assertFalse(accounts.first().scrobble(play).isSuccess)
        assertEquals("expired", f.prefs.data.first().atprotoSession?.accessJwt)
        f.refreshStatus = 200
        f.database().use { db ->
            f.dispatch(db, play, accounts)
            assertEquals(0, db.getPendingScrobblesDao().count())
        }
        assertEquals(2, f.rotations.get()) // one outage, one shared successful rotation
        assertEquals("active", f.prefs.data.first().atprotoSession?.accessJwt)
        assertEquals(2, f.records.size)
        assertTrue(f.reads.isEmpty())
    }

    private inline fun PanoDb.use(block: (PanoDb) -> Unit) = try {
        block(this)
    } finally {
        close()
    }

    private fun fixture(block: suspend (Fixture) -> Unit) = runBlocking {
        val dir = createTempDirectory("at-play-").toFile()
        DesktopStuff.parseCmdlineArgs(arrayOf("--data-dir", dir.path))
        val job = Job()
        try {
            HttpClient(OkHttp) { followRedirects = false }.use { http ->
                val fixture = Fixture(dir, CoroutineScope(Dispatchers.IO + job), http)
                try {
                    block(fixture)
                } finally {
                    fixture.server.stop(0)
                }
            }
        } finally {
            job.cancelAndJoin()
            dir.deleteRecursively()
        }
        Unit
    }

    private data class Post(val uri: String, val collection: String, val key: String, val record: JsonObject)

    private sealed interface Failure {
        data object DropReply : Failure
        data object Unavailable : Failure
        data class Collision(val existing: JsonObject) : Failure
    }

    private class Fixture(private val dir: File, scope: CoroutineScope, private val http: HttpClient) {
        val prefs = DataStoreFactory.create(MainPrefs.dataStoreSerializer, scope = scope) { dir.resolve("prefs.json") }
        private val sessions = AtprotoSessions(prefs)
        val csv = dir.resolve("plays.csv").apply { createNewFile() }
        val fileAdapter = FileScrobblable(UserAccountSerializable(AccountType.FILE, user, "csv", csv.toURI().toString()))
        val scheduled = mutableListOf<Boolean>()
        val rotations = AtomicInteger()
        val posts = ConcurrentLinkedQueue<Post>()
        val reads = ConcurrentLinkedQueue<String>()
        val records = ConcurrentHashMap<String, JsonObject>()
        val failures = ConcurrentHashMap<Pair<String, String>, Failure>()
        @Volatile var refreshStatus = 200

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).also { server ->
            server.createContext("/xrpc/") { request ->
                request.use {
                    val auth = request.requestHeaders.getFirst("Authorization")
                    when (request.requestURI.path.substringAfterLast('.')) {
                        "putRecord" -> putRecord(request, auth)
                        "getRecord" -> {
                            val q = request.requestURI.rawQuery.split('&').associate {
                                it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), Charsets.UTF_8)
                            }
                            val uri = "at://${q["repo"]}/${q["collection"]}/${q["rkey"]}"
                            reads += uri
                            val saved = records[uri]
                            when {
                                auth != null -> request.respond(400, "{}")
                                saved == null -> request.respond(400, """{"error":"RecordNotFound"}""")
                                else -> request.respond(200, """{"uri":"$uri","cid":"$cid","value":$saved}""")
                            }
                        }
                        "refreshSession" -> {
                            rotations.incrementAndGet()
                            if (auth == "Bearer fixture-refresh")
                                request.respond(refreshStatus, """{"did":"$did","accessJwt":"active","refreshJwt":"rotated"}""")
                            else request.respond(400, """{"error":"InvalidToken"}""")
                        }
                        else -> request.respond(404, "{}")
                    }
                }
            }
            server.start()
        }

        private fun putRecord(request: HttpExchange, auth: String?) {
            val payload = Json.parseToJsonElement(request.requestBody.readAllBytes().decodeToString()).jsonObject
            val collection = payload.getValue("collection").jsonPrimitive.content
            val key = payload.getValue("rkey").jsonPrimitive.content
            val uri = "at://${payload.getValue("repo").jsonPrimitive.content}/$collection/$key"
            val record = payload.getValue("record").jsonObject
            val title = record.getValue(if (collection == teal) "trackName" else "title").jsonPrimitive.content
            posts += Post(uri, collection, key, record)
            if (payload["swapRecord"] != JsonNull) {
                request.respond(400, "{}") // Refuse anything but explicit create-only writes.
                return
            }
            if (auth != "Bearer active") {
                request.respond(400, """{"error":"ExpiredToken"}""")
                return
            }
            val failure = failures.remove(collection to title)
            if (failure is Failure.Collision) records[uri] = failure.existing
            when {
                failure == Failure.Unavailable -> request.respond(502, "{}")
                records.putIfAbsent(uri, record) != null -> request.respond(400, """{"error":"InvalidSwap"}""")
                failure == Failure.DropReply -> Unit
                else -> request.respond(200, """{"uri":"$uri","cid":"$cid"}""")
            }
        }

        suspend fun connect(accessJwt: String = "active") {
            val pds = "http://127.0.0.1:${server.address.port}"
            sessions.connect(AtprotoAppPasswordSession(did, pds, accessJwt, "fixture-refresh"), user, AccountType.TEAL)
            sessions.connectSaved(did, AccountType.ROCKSKY)
        }

        suspend fun adapters(): List<Scrobblable> = prefs.data.first().scrobbleAccounts.map { account ->
            AtprotoScrobblable(account, { http }, { http }, { AtprotoSessions(prefs) })
        }

        fun database() = Room.databaseBuilder<PanoDb>(dir.resolve("pano.db").path).setDriver(BundledSQLiteDriver()).build()

        suspend fun dispatch(db: PanoDb, data: ScrobbleData, accounts: List<Scrobblable>) =
            ScrobbleEverywhere.scrobble(data, db, { accounts }, { scheduled += it })

        suspend fun work(db: PanoDb, accounts: List<Scrobblable>) =
            PendingScrobblesWorker({}, { db }, { accounts }).doWork()

        private fun HttpExchange.respond(status: Int, value: String) {
            val bytes = value.encodeToByteArray()
            responseHeaders.set("Content-Type", "application/json")
            sendResponseHeaders(status, bytes.size.toLong())
            responseBody.use { it.write(bytes) }
        }
    }

    private companion object {
        const val did = "did:plc:abcdefghijklmnopqrstuvwx"
        const val cid = "bafyreihtjrpllq4g265etmj5uo5chx24pbi4mnawz4vr4zugasbv62ed5u"
        val teal = AccountType.TEAL.playCollection
        val rocksky = AccountType.ROCKSKY.playCollection
        val user = UserCached("fixture.example", "", "", "", 0)
        val play = ScrobbleData(
            "Fixture & Artist", "Étude", "Fixture album", 1_756_245_123_456,
            trackNumber = 3, albumArtist = "Fixture ensemble", duration = 182_320, appId = "test.player",
            extraTags = mapOf("ISRC" to "XXAAA2400001"),
        )

        fun decodeTidTime(key: String): Long {
            assertTrue(key.matches(Regex("[2-7a-j][2-7a-z]{12}")))
            return key.fold(0L) { value, ch -> (value shl 5) or "234567abcdefghijklmnopqrstuvwxyz".indexOf(ch).toLong() } ushr 10
        }
    }
}
