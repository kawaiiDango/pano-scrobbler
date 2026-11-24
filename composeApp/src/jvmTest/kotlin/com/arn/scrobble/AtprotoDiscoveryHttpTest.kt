package com.arn.scrobble

import com.arn.scrobble.api.atproto.getIdentityDocument
import com.arn.scrobble.api.lastfm.ApiException
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AtprotoDiscoveryHttpTest {
    @Test
    fun limitsDiscoveryReadsAndReportsHttpErrors() = runBlocking {
        val maxBytes = 512 * 1024
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/error") {
            it.sendResponseHeaders(429, -1)
            it.close()
        }
        server.createContext("/body") {
            val bytes = ByteArray(it.requestURI.query.toInt()) { 32 }
            it.sendResponseHeaders(200, 0) // Chunked: no Content-Length to rely on.
            it.responseBody.use { body -> body.write(bytes) }
        }
        server.start()
        try {
            val url = "http://127.0.0.1:${server.address.port}"
            HttpClient(OkHttp).use { client ->
                assertEquals("   ", client.getIdentityDocument("$url/body?3"))
                assertEquals(maxBytes, client.getIdentityDocument("$url/body?$maxBytes").length)
                assertFailsWith<IllegalArgumentException> {
                    client.getIdentityDocument("$url/body?${maxBytes + 1}")
                }
                val error = assertFailsWith<ApiException> { client.getIdentityDocument("$url/error") }
                assertEquals(429, error.code)
            }
        } finally {
            server.stop(0)
        }
    }
}
