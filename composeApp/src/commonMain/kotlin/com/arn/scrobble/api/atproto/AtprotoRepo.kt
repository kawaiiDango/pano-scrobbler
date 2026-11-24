package com.arn.scrobble.api.atproto

import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.utils.Stuff
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readBuffer
import kotlinx.io.readString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

internal class AtprotoXrpcError(status: Int, error: String?) : ApiException(status, "AT Protocol request failed") {
    val rejectedToken = code in 400..401 && error in setOf("ExpiredToken", "InvalidToken", "invalid_token")
    val recordCollision = code in setOf(400, 409) && error == "InvalidSwap"
}

private suspend inline fun <reified T> HttpResponse.repoJson(): T {
    val limit = 128 * 1024L
    val buf = bodyAsChannel().readBuffer(limit + 1)
    if (buf.size > limit) throw ApiException(-1, "AT Protocol response too large")
    val body = buf.readString()
    if (!status.isSuccess()) {
        val error = runCatching { Stuff.myJson.decodeFromString<JsonObject>(body)["error"] as? JsonPrimitive }.getOrNull()
        throw AtprotoXrpcError(status.value, error?.content)
    }
    return try {
        Stuff.myJson.decodeFromString<T>(body)
    } catch (_: kotlinx.serialization.SerializationException) {
        throw ApiException(-1, "Invalid AT Protocol response")
    }
}

@Serializable
private data class RepoRecord(val uri: String, val cid: String, val value: JsonObject? = null)

/** Ask the authoritative PDS about this exact key, not the service's search index. */
private suspend fun HttpClient.getPlay(pds: String, did: String, collection: String, rkey: String): RepoRecord =
    prepareGet("$pds/xrpc/com.atproto.repo.getRecord") {
        parameter("repo", did)
        parameter("collection", collection)
        parameter("rkey", rkey)
        header(HttpHeaders.CacheControl, "no-cache")
    }.execute { it.repoJson<RepoRecord>() }

private val repoCid = Regex("bafyrei[a-z2-7]{51}[aeimquy4]")

private fun RepoRecord.checkAddress(did: String, collection: String, rkey: String) {
    if (uri != "at://$did/$collection/$rkey" || !repoCid.matches(cid))
        throw ApiException(-1, "PDS returned a receipt for an invalid record address or CID")
}

private fun RepoRecord.matchesPlay(record: JsonObject): Boolean {
    val timestamp = if ("playedTime" in record) "playedTime" else "createdAt"
    val saved = value ?: return false
    fun JsonObject.time() = (get(timestamp) as? JsonPrimitive)?.content?.let { runCatching { Instant.parse(it) }.getOrNull() }
    return record.time() != null && record.time() == saved.time() && (record - timestamp) == (saved - timestamp)
}

internal suspend fun HttpClient.createPlay(
    session: AtprotoAppPasswordSession, collection: String, rkey: String, record: JsonObject,
) {
    val payload = buildJsonObject {
        put("repo", session.did)
        put("collection", collection)
        put("rkey", rkey)
        put("record", record)
        // Explicit null requires an absent record. Omitting it would allow an overwrite.
        put("swapRecord", JsonNull)
    }.toString()
    val saved = try {
        preparePost("${session.pds}/xrpc/com.atproto.repo.putRecord") {
            header(HttpHeaders.Authorization, "Bearer ${session.accessJwt}")
            setBody(AtprotoPostContent(payload, ContentType.Application.Json))
        }.execute { it.repoJson<RepoRecord>() }
    } catch (e: AtprotoXrpcError) {
        if (!e.recordCollision) throw e
        getPlay(session.pds, session.did, collection, rkey).also {
            if (!it.matchesPlay(record)) throw AtprotoPlayRejected("A different $collection record already uses this key")
        }
    }
    saved.checkAddress(session.did, collection, rkey)
}

@Serializable
private class AppPasswordRotation(val did: String, val accessJwt: String, val refreshJwt: String, val active: Boolean = true)

internal suspend fun HttpClient.rotateAppPassword(pds: String, did: String, token: String): AtprotoAppPasswordSession {
    if (!token.isBearerToken()) throw AtprotoReconnect()
    val result = preparePost("$pds/xrpc/com.atproto.server.refreshSession") {
        header(HttpHeaders.Authorization, "Bearer $token")
        setBody(AtprotoPostContent("", ContentType.Application.Json))
    }.execute { it.repoJson<AppPasswordRotation>() }
    if (result.did != did || !result.active || !result.accessJwt.isBearerToken() || !result.refreshJwt.isBearerToken())
        throw AtprotoReconnect()
    return AtprotoAppPasswordSession(did, pds, result.accessJwt, result.refreshJwt)
}
