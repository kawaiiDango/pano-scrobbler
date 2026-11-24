package com.arn.scrobble

import com.arn.scrobble.api.atproto.AtprotoIdentity
import com.arn.scrobble.api.atproto.signInWithAppPassword
import com.arn.scrobble.api.lastfm.ApiException
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AtprotoAppPasswordHttpTest {
    private val did = "did:plc:abcdefghijklmnopqrstuvwx"
    private val password = "test-password-\"é"
    private val sessionPath = "/xrpc/com.atproto.server.createSession"
    private val sessionJson = """{"did":"$did","accessJwt":"test-access","refreshJwt":"test-refresh"}"""

    @Test
    fun exchangesPasswordUsingResolvedDidAndPds() = runBlocking {
        val received = AtomicReference<List<String>>()
        val session = exchange { request ->
            received.set(listOf(
                request.requestMethod,
                request.requestURI.path,
                request.requestHeaders.getFirst("Content-Type").orEmpty(),
                request.requestBody.readBytes().decodeToString(),
            ))
            request.respond(200, sessionJson)
        }
        val (method, path, contentType, body) = received.get()
        assertEquals("POST", method)
        assertEquals(sessionPath, path)
        assertEquals("application/json", contentType)
        assertEquals(buildJsonObject {
            put("identifier", did)
            put("password", password)
        }, Json.parseToJsonElement(body))
        assertEquals(did, session.did)
        assertEquals("test-access", session.accessJwt)
        assertEquals("test-refresh", session.refreshJwt)
        assertFalse("test-access" in session.toString())
        assertFalse("test-refresh" in session.toString())
    }

    @Test
    fun rejectsSessionForAnotherDid() = runBlocking {
        val error = assertFailsWith<ApiException> {
            exchange { it.respond(200, sessionJson.replace(did, "did:plc:bcdefghijklmnopqrstuvwxy")) }
        }
        assertEquals("PDS did not return an active session for this account", error.message)
    }

    @Test
    fun redactsMalformedSessionResponse() = runBlocking {
        val error = assertFailsWith<ApiException> {
            exchange { it.respond(200, sessionJson.dropLast(1)) }
        }
        assertEquals("Invalid AT Protocol session response", error.message)
        assertNull(error.cause)
    }

    @Test
    fun doesNotFollowCredentialPostRedirect() = runBlocking {
        val paths = ConcurrentLinkedQueue<String>()
        val error = assertFailsWith<ApiException> {
            exchange { request ->
                val path = request.requestURI.path
                paths.add(path)
                if (path == sessionPath) {
                    check(request.requestBody.readBytes().isNotEmpty())
                    request.responseHeaders.add("Location", "/redirect-target")
                    request.respond(307)
                } else {
                    request.respond(200, sessionJson)
                }
            }
        }
        assertEquals(307, error.code)
        assertEquals(listOf(sessionPath), paths.toList())
    }

    @Test
    fun doesNotReplayCredentialPostOnRetryAfter() = runBlocking {
        val posts = AtomicInteger()
        val error = assertFailsWith<ApiException> {
            exchange { request ->
                posts.incrementAndGet()
                check(request.requestBody.readBytes().isNotEmpty())
                request.responseHeaders.add("Retry-After", "0")
                request.respond(503)
            }
        }
        assertEquals(503, error.code)
        assertEquals(1, posts.get())
    }

    private suspend fun exchange(handler: (HttpExchange) -> Unit) =
        HttpClient(OkHttp).use { client ->
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            server.createContext("/") { it.use(handler) }
            server.start()
            try {
                val pds = "http://127.0.0.1:${server.address.port}"
                AtprotoIdentity(did, pds).signInWithAppPassword(password, client).also {
                    assertEquals(pds, it.pds)
                }
            } finally {
                server.stop(0)
            }
        }

    private fun HttpExchange.respond(status: Int, body: String = "") {
        if (body.isEmpty()) {
            sendResponseHeaders(status, -1)
        } else {
            responseHeaders.set("Content-Type", "application/json")
            val bytes = body.encodeToByteArray()
            sendResponseHeaders(status, bytes.size.toLong())
            responseBody.use { it.write(bytes) }
        }
    }
}
