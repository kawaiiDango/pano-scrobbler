package com.arn.scrobble.api.atproto

import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.utils.Stuff
import io.ktor.client.HttpClient
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readBuffer
import kotlinx.io.readString
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

@Serializable
class AtprotoAppPasswordSession(
    val did: String,
    val pds: String,
    val accessJwt: String,
    val refreshJwt: String,
) {
    override fun toString() = "AtprotoAppPasswordSession(did=$did, pds=$pds)"
}

@Serializable
private class CreateSessionRequest(val identifier: String, val password: String)

@Serializable
private class CreateSessionResponse(
    val did: String,
    val accessJwt: String,
    val refreshJwt: String,
    val active: Boolean = true,
)

/** Resolves the handle, exchanges the app password at its verified PDS, and checks the session's DID. */
suspend fun signInWithAppPassword(handle: String, password: String): AtprotoAppPasswordSession =
    resolveAtprotoHandle(handle).exchangeAppPassword(Requesters.atprotoPdsClient, password)

suspend fun AtprotoIdentity.signInWithAppPassword(
    password: String,
    baseClient: HttpClient,
): AtprotoAppPasswordSession =
    baseClient.config { followRedirects = false }.use { client ->
        exchangeAppPassword(client, password)
    }

private suspend fun AtprotoIdentity.exchangeAppPassword(
    client: HttpClient,
    password: String,
): AtprotoAppPasswordSession {
    val credentials = Stuff.myJson.encodeToString(CreateSessionRequest(did, password))
    return client.preparePost("$pds/xrpc/com.atproto.server.createSession") {
        setBody(AtprotoPostContent(credentials, ContentType.Application.Json))
    }.execute { response ->
        if (!response.status.isSuccess())
            throw ApiException(response.status.value, response.status.description)
        val limit = 128 * 1024L
        val body = response.bodyAsChannel().readBuffer(limit + 1)
        if (body.size > limit) throw ApiException(-1, "AT Protocol session response too large")
        val session = try {
            Stuff.myJson.decodeFromString<CreateSessionResponse>(body.readString())
        } catch (_: SerializationException) {
            // A parser exception can include the response's session tokens.
            throw ApiException(-1, "Invalid AT Protocol session response")
        }
        if (!session.active || session.did != did ||
            !session.accessJwt.isBearerToken() || !session.refreshJwt.isBearerToken())
            throw ApiException(-1, "PDS did not return an active session for this account")
        AtprotoAppPasswordSession(session.did, pds, session.accessJwt, session.refreshJwt)
    }
}

internal fun String.isBearerToken() = isNotEmpty() && all { it in '!'..'~' }
