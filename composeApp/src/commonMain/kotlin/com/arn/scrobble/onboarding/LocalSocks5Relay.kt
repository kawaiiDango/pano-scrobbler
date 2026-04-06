package com.arn.scrobble.onboarding

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

/**
 * A minimal local SOCKS5 proxy that accepts **unauthenticated** connections
 * on 127.0.0.1 and forwards them to an upstream SOCKS5 proxy using
 * username/password authentication (RFC 1928 + RFC 1929).
 *
 * This exists because [androidx.webkit.ProxyController] and WebView2 (Windows)
 * support `socks://host:port` but have no way to supply credentials.
 */
class Socks5ProxyTunnel(
    private val upstreamHost: String,
    private val upstreamPort: Int,
    private val username: String,
    private val password: String,
    private val scope: CoroutineScope,
) : Closeable {

    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    /** The local port clients should connect to (available after [start]). */
    val localPort: Int
        get() = serverSocket?.localPort
            ?: throw IllegalStateException("Tunnel not started")

    fun start(): Int {
        val ss = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress("127.0.0.1", 0)) // OS-assigned port
        }
        serverSocket = ss

        acceptJob = scope.launch(Dispatchers.IO) {
            Logger.i { "Socks5ProxyTunnel listening on 127.0.0.1:${ss.localPort}" }
            while (isActive) {
                try {
                    val client = ss.accept()
                    launch { handleClient(client) }
                } catch (_: SocketException) {
                    break // serverSocket closed
                }
            }
        }

        return ss.localPort
    }

    override fun close() {
        acceptJob?.cancel()
        serverSocket?.close()
        serverSocket = null
    }

    // ── per-connection handling ──────────────────────────────────────────

    private suspend fun handleClient(client: Socket) = withContext(Dispatchers.IO) {
        try {
            client.use { clientSock ->
                val cIn = clientSock.getInputStream()
                val cOut = clientSock.getOutputStream()

                // 1. Read the local (no-auth) SOCKS5 greeting from the client
                val clientGreeting = readGreeting(cIn)
                if (clientGreeting.version != 5.toByte()) {
                    sendNoAcceptable(cOut)
                    return@withContext
                }

                // Reply: we accept NO-AUTH (0x00)
                cOut.write(byteArrayOf(0x05, 0x00))
                cOut.flush()

                // 2. Read the SOCKS5 request (CONNECT / BIND / UDP ASSOCIATE)
                val request = readRaw(cIn) // variable-length, we'll forward as-is

                // 3. Open connection to the real upstream proxy
                val upstream = Socket()
                upstream.connect(InetSocketAddress(upstreamHost, upstreamPort), 10_000)

                upstream.use { upSock ->
                    val uIn = upSock.getInputStream()
                    val uOut = upSock.getOutputStream()

                    // 4. Perform authenticated handshake with upstream
                    //    Offer USERNAME/PASSWORD (0x02)
                    uOut.write(byteArrayOf(0x05, 0x01, 0x02))
                    uOut.flush()

                    val uVer = uIn.read()
                    val uMethod = uIn.read()
                    if (uVer != 0x05 || uMethod != 0x02) {
                        sendGeneralFailure(cOut)
                        return@withContext
                    }

                    // RFC 1929 sub-negotiation
                    val userBytes = username.toByteArray(Charsets.UTF_8)
                    val passBytes = password.toByteArray(Charsets.UTF_8)
                    val authPacket = ByteArray(3 + userBytes.size + passBytes.size).also {
                        it[0] = 0x01 // sub-negotiation version
                        it[1] = userBytes.size.toByte()
                        userBytes.copyInto(it, 2)
                        it[2 + userBytes.size] = passBytes.size.toByte()
                        passBytes.copyInto(it, 3 + userBytes.size)
                    }
                    uOut.write(authPacket)
                    uOut.flush()

                    val authVer = uIn.read()
                    val authStatus = uIn.read()
                    if (authVer != 0x01 || authStatus != 0x00) {
                        sendGeneralFailure(cOut)
                        return@withContext
                    }

                    // 5. Forward the original CONNECT request to upstream
                    uOut.write(request)
                    uOut.flush()

                    // 6. Read the upstream reply and forward it back to client
                    val upstreamReply = readRaw(uIn)
                    cOut.write(upstreamReply)
                    cOut.flush()

                    // Check if CONNECT succeeded (reply[1] == 0x00)
                    if (upstreamReply.size < 2 || upstreamReply[1] != 0x00.toByte()) {
                        return@withContext
                    }

                    // 7. Bi-directional relay
                    val job1 = launch { relay(cIn, uOut) }
                    val job2 = launch { relay(uIn, cOut) }
                    job1.join()
                    job2.join()
                }
            }
        } catch (e: Exception) {
            Logger.w(e) { "Socks5ProxyTunnel client handler error" }
        }
    }

    // ── SOCKS5 helpers ──────────────────────────────────────────────────

    /** Reads the initial SOCKS5 greeting (version + methods). */
    private class Greeting(val version: Byte, val methods: ByteArray)

    private fun readGreeting(input: InputStream): Greeting {
        val ver = input.read().toByte()
        val nMethods = input.read()
        val methods = ByteArray(nMethods)
        input.readFully(methods)
        return Greeting(ver, methods)
    }

    /**
     * Reads a full SOCKS5 request or reply packet.
     * Format: VER(1) CMD/REP(1) RSV(1) ATYP(1) ADDR(variable) PORT(2)
     */
    private fun readRaw(input: InputStream): ByteArray {
        val header = ByteArray(4)
        input.readFully(header)

        val atyp = header[3].toInt() and 0xFF
        val addrBytes: ByteArray = when (atyp) {
            0x01 -> ByteArray(4).also { input.readFully(it) }    // IPv4
            0x04 -> ByteArray(16).also { input.readFully(it) }   // IPv6
            0x03 -> {                                              // Domain
                val len = input.read()
                ByteArray(1 + len).also {
                    it[0] = len.toByte()
                    input.readFully(it, 1, len)
                }
            }

            else -> throw IllegalStateException("Unknown ATYP: $atyp")
        }

        val port = ByteArray(2)
        input.readFully(port)

        return header + addrBytes + port
    }

    private fun sendNoAcceptable(out: OutputStream) {
        out.write(byteArrayOf(0x05, 0xFF.toByte()))
        out.flush()
    }

    private fun sendGeneralFailure(out: OutputStream) {
        // VER=5, REP=1 (general failure), RSV=0, ATYP=1, ADDR=0.0.0.0, PORT=0
        out.write(byteArrayOf(0x05, 0x01, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
        out.flush()
    }

    private fun InputStream.readFully(buf: ByteArray, off: Int = 0, len: Int = buf.size) {
        var read = 0
        while (read < len) {
            val n = this.read(buf, off + read, len - read)
            if (n < 0) throw SocketException("Unexpected end of stream")
            read += n
        }
    }

    private fun relay(input: InputStream, output: OutputStream) {
        try {
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                output.write(buf, 0, n)
                output.flush()
            }
        } catch (_: SocketException) {
            // connection closed
        } catch (_: Exception) {
            // ignore relay errors
        }
    }
}