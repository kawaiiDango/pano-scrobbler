package com.arn.scrobble.utils

import androidx.compose.runtime.Composable
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import javax.net.ssl.SSLHandshakeException

class LocalNetworkHandshakeExceptionWrapper(wrapped: Throwable) :
    SSLHandshakeException(wrapped.message) {
    init {
        initCause(wrapped)
    }
}

class LocalNetworkPermissionNeededException : IOException("Cannot connect to local network")

suspend fun isLocal(url: Url): Boolean {
    val host = url.host

    if (host.endsWith(".local", ignoreCase = true)) return true
    if (host.equals("localhost", ignoreCase = true)) return true

    val addresses = withContext(Dispatchers.IO) {
        try {
            InetAddress.getAllByName(host)
        } catch (_: Exception) {
            return@withContext emptyArray<InetAddress>()
        }
    }

    for (address in addresses) {
        if (address.isLoopbackAddress) return true
        val b = address.address
        if (b.size == 4) {
            val a0 = b[0].toInt() and 0xFF
            val a1 = b[1].toInt() and 0xFF
            return when (a0) {
                10 -> true                        // 10.0.0.0/8
                172 if a1 in 16..31 -> true       // 172.16.0.0/12
                192 if a1 == 168 -> true          // 192.168.0.0/16
                169 if a1 == 254 -> true          // 169.254.0.0/16 link-local
                else -> false
            }
        }
        if (b.size == 16) {
            // fc00::/7 (ULA) and fe80::/10 (link-local)
            val a0 = b[0].toInt() and 0xFF
            return (a0 and 0xFE) == 0xFC || (a0 == 0xFE && (b[1].toInt() and 0xC0) == 0x80)
        }
    }
    return false
}

suspend fun wrapLocalNetworkHandshakeException(e: Throwable, url: String?): Throwable? {
    fun Throwable.findHandshakeException(): Throwable? {
        var t: Throwable? = this
        while (t != null) {
            if (t is SSLHandshakeException)
                return t
            t = t.cause
        }
        return null
    }

    val handshakeException = e.findHandshakeException() ?: return null
    if (e is LocalNetworkHandshakeExceptionWrapper) return null
    url ?: return null

    if (isLocal(Url(url)))
        return LocalNetworkHandshakeExceptionWrapper(handshakeException)
    return null
}

expect suspend fun determineLocalNetworkPermissionException(url: String?): Throwable?

@Composable
expect fun LocalNetworkPermissionsRequest(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
)