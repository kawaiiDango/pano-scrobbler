package com.arn.scrobble.api.atproto

import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.utils.Stuff
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readBuffer
import kotlinx.io.readString
import kotlinx.serialization.Serializable
import java.net.URI

data class AtprotoIdentity(val did: String, val pds: String)

private val label = "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"
private val handlePattern = Regex("(?:$label\\.)+[a-z](?:[a-z0-9-]{0,61}[a-z0-9])?")

@Serializable
private data class HandleResolution(val did: String)

@Serializable
private data class DidDocument(
    val id: String,
    val alsoKnownAs: List<String> = emptyList(),
    val service: List<Service> = emptyList(),
) {
    @Serializable
    data class Service(val id: String, val type: String, val serviceEndpoint: String)
}

suspend fun HttpClient.getIdentityDocument(url: String): String =
    prepareGet(url).execute { response ->
        if (!response.status.isSuccess())
            throw ApiException(response.status.value, response.status.description)
        val maxSize = 512 * 1024L
        val body = response.bodyAsChannel().readBuffer(maxSize + 1)
        require(body.size <= maxSize) { "Identity document too large" }
        body.readString()
    }

/**
 * Resolves a handle to a DID (decentralized identifier), the account's stable ID even if its handle changes.
 * The DID document must claim the handle and name an HTTPS PDS (Personal Data Server), which hosts the account's records.
 */
suspend fun resolveAtprotoHandle(
    input: String,
    getDocument: suspend (String) -> String = { url ->
        Requesters.atprotoPdsClient.getIdentityDocument(url)
    },
): AtprotoIdentity {
    val handle = input.trim().removePrefix("@").lowercase()
    require(handle.length <= 253 && handlePattern.matches(handle)) { "Invalid handle" }
    val resolution = getDocument("https://public.api.bsky.app/xrpc/com.atproto.identity.resolveHandle?handle=$handle")
    val did = Stuff.myJson.decodeFromString<HandleResolution>(resolution).did
    val url = when {
        did.matches(Regex("did:plc:[a-z2-7]{24}")) -> "https://plc.directory/$did"
        did.startsWith("did:web:") -> {
            val host = did.removePrefix("did:web:")
            require(host.length <= 253 && handlePattern.matches(host.lowercase())) { "Invalid did:web host" }
            "https://$host/.well-known/did.json"
        }
        else -> throw IllegalArgumentException("Unsupported DID")
    }
    val doc = Stuff.myJson.decodeFromString<DidDocument>(getDocument(url))
    require(doc.id == did && doc.alsoKnownAs.firstOrNull { it.startsWith("at://") }
        .equals("at://$handle", ignoreCase = true)) { "Handle does not match DID document" }
    val pds = doc.service.singleOrNull { (it.id == "#atproto_pds" || it.id == "$did#atproto_pds") &&
        it.type == "AtprotoPersonalDataServer" } ?: throw IllegalArgumentException("Missing PDS")
    val endpoint = URI(pds.serviceEndpoint)
    require(endpoint.scheme == "https" && endpoint.host != null && endpoint.rawUserInfo == null &&
        endpoint.rawQuery == null && endpoint.rawFragment == null && endpoint.rawPath in listOf("", "/") &&
        (endpoint.port == -1 || endpoint.port in 1..65535)) { "Invalid PDS endpoint" }
    return AtprotoIdentity(did, pds.serviceEndpoint.trimEnd('/'))
}
