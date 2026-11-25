package com.arn.scrobble.api.teal

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.IOException
import java.util.UUID
import kotlin.time.Instant

/** The app password is used only at login; only the returned session is persisted. */
@Serializable
internal data class TealSession(
    val did: String,
    val handle: String,
    val pds: String,
    val accessJwt: String,
    val refreshJwt: String,
    val id: String = UUID.randomUUID().toString(),
) {
    override fun toString() = "TealSession(did=$did)"
}

internal interface TealSessionStore {
    // A persisted store is bound to one login and rejects logout or login replacement on load/save.
    suspend fun load(): TealSession
    suspend fun save(session: TealSession)
}

internal class TealRecordCollisionException : IOException("A different listen already uses this record key.")

internal class TealApiException(val error: String, val status: Int) : IOException(
    when {
        error == "AuthFactorTokenRequired" -> "Use an app password, not your account password."
        error in setOf("AuthenticationRequired", "InvalidToken", "ExpiredToken") || status == 401 ->
            "teal.fm session expired or app password rejected. Reconnect in Settings → Accounts using an app password."
        status == 429 -> "Your PDS is rate limiting requests. Please try again later."
        else -> "PDS request failed ($status, ${error.take(80)})."
    }
)

internal class TealApiClient(
    private val withHttp: suspend (suspend (HttpClient) -> JsonObject) -> JsonObject,
    private val store: TealSessionStore? = null,
    private val refreshCoordinator: TealRefreshCoordinator = TealRefreshCoordinator.processLocal,
) {
    constructor(http: HttpClient, store: TealSessionStore? = null) : this({ request -> request(http) }, store)

    private suspend fun refresh(initial: TealSession, store: TealSessionStore): TealSession =
        refreshCoordinator.rotate {
            val current = store.load()
            check(current.did == initial.did && current.pds == initial.pds) {
                "teal.fm account changed. Retry with the current account."
            }
            if (current.accessJwt != initial.accessJwt) return@rotate current

            val refreshed = request(current.pds, "com.atproto.server.refreshSession", null, current.refreshJwt)
            require(refreshed.string("did") == current.did) { "PDS returned a different account" }
            val rotated = current.copy(
                handle = refreshed.string("handle"),
                accessJwt = refreshed.string("accessJwt"),
                refreshJwt = refreshed.string("refreshJwt"),
            )
            store.save(rotated)
            rotated
        }

    suspend fun login(identifier: String, appPassword: String): TealSession {
        require(identifier.isNotBlank() && appPassword.isNotBlank()) { "Enter your handle and app password." }
        val input = identifier.trim().removePrefix("@")
        val did = run {
            require(input.matches(Regex("[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))) {
                "Enter your full handle, such as ilovetealfm.bsky.social."
            }
            get("https://public.api.bsky.app/xrpc/com.atproto.identity.resolveHandle", mapOf("handle" to input.lowercase())).string("did")
        }
        val docUrl = when {
            did.startsWith("did:plc:") && did.removePrefix("did:plc:").matches(Regex("[a-z2-7]{24}")) -> "https://plc.directory/$did"
            did.startsWith("did:web:") && did.removePrefix("did:web:").matches(Regex("[a-zA-Z0-9.-]+")) ->
                "https://${did.removePrefix("did:web:")}/.well-known/did.json"
            else -> throw IllegalArgumentException("Unsupported or invalid DID.")
        }
        val doc = get(docUrl)
        require(doc.string("id") == did) { "Identity document does not match the account." }
        require(doc["alsoKnownAs"]?.jsonArray?.any { it.jsonPrimitive.content == "at://${input.lowercase()}" } == true) {
            "The handle does not match its identity document. Check your handle and try again."
        }
        val service = doc["service"]?.jsonArray?.map { it.jsonObject }?.firstOrNull {
            it["id"]?.jsonPrimitive?.content in setOf("#atproto_pds", "$did#atproto_pds") &&
                it["type"]?.jsonPrimitive?.content == "AtprotoPersonalDataServer"
        } ?: throw IllegalArgumentException("No PDS was found for this account.")
        val pds = httpsOrigin(service.string("serviceEndpoint"))
        val response = request(pds, "com.atproto.server.createSession", buildJsonObject {
            put("identifier", did)
            put("password", appPassword)
        })
        require(response.string("did") == did) { "PDS returned a different account." }
        require(response["active"]?.jsonPrimitive?.booleanOrNull != false) { "This account is not active." }
        return TealSession(did, response.string("handle"), pds,
            response.string("accessJwt"), response.string("refreshJwt"))
    }

    suspend fun write(collection: String, rkey: String, record: JsonObject, createOnly: Boolean = false): JsonObject {
        val store = checkNotNull(store)
        val initial = store.load()
        val body = buildJsonObject {
            put("repo", initial.did)
            put("collection", collection)
            put("rkey", rkey)
            put("record", record)
            // Explicit null is a compare-and-swap precondition: do not replace an existing play.
            if (createOnly) put("swapRecord", JsonNull)
        }
        suspend fun putRecord(session: TealSession) = try {
            Result.success(request(session.pds, "com.atproto.repo.putRecord", body, session.accessJwt))
        } catch (e: TealApiException) {
            Result.failure(e)
        }

        var session = initial
        var attempt = putRecord(session)
        val error = attempt.exceptionOrNull() as? TealApiException
        if (error?.error == "ExpiredToken" && (error.status == 400 || error.status == 401)) {
            session = refresh(initial, store)
            attempt = putRecord(session)
        }
        return attempt.getOrElse { failure ->
            if (!createOnly || failure !is TealApiException || failure.status != 400 || failure.error != "InvalidSwap")
                throw failure
            reconcilePlay(session, collection, rkey, record)
        }
    }

    private suspend fun reconcilePlay(session: TealSession, collection: String, rkey: String, record: JsonObject): JsonObject {
        val existing = getRecord(session.pds, session.did, collection, rkey)
        existing.string("uri")
        val value = existing["value"] as? JsonObject
            ?: throw IOException("PDS returned an invalid existing record.")
        val track = value["trackName"] as? JsonPrimitive
        val time = value["playedTime"] as? JsonPrimitive
        val artists = value["artists"] as? JsonArray
        if ((value["\$type"] as? JsonPrimitive)?.let { it.isString && it.content == TEAL_PLAY } != true ||
            track?.isString != true || track.content.isBlank() ||
            time?.isString != true || Instant.parseOrNull(time.content) == null || artists == null ||
            !value.hasValidTealOptionalTrackFields() ||
            artists.any {
                val name = (it as? JsonObject)?.get("artistName") as? JsonPrimitive
                name?.isString != true || name.content.isBlank()
            })
            throw IOException("PDS returned an invalid existing record.")

        fun identity(value: JsonObject): JsonObject = buildJsonObject {
            value.filterKeys { it !in setOf("submissionClientAgent", "recordingMbId", "releaseMbId") }
                .forEach { (key, item) ->
                    put(key, if (key == "artists") JsonArray(item.jsonArray.map { artist ->
                        JsonObject(artist.jsonObject.filterKeys { it != "artistMbId" })
                    }) else item)
                }
        }
        if (identity(value) != identity(record)) throw TealRecordCollisionException()
        return existing
    }

    suspend fun listRecords(pds: String, did: String, collection: String, limit: Int, cursor: String?) =
        get("$pds/xrpc/com.atproto.repo.listRecords", buildMap {
            put("repo", did)
            put("collection", collection)
            put("limit", limit.coerceIn(1, 100).toString())
            cursor?.let { put("cursor", it) }
        })

    suspend fun getRecord(pds: String, did: String, collection: String, rkey: String) =
        get("$pds/xrpc/com.atproto.repo.getRecord", mapOf("repo" to did, "collection" to collection, "rkey" to rkey))

    private suspend fun request(pds: String, method: String, body: JsonObject?, token: String? = null): JsonObject = withHttp { http ->
        val response = http.post("${httpsOrigin(pds)}/xrpc/$method") {
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
            token?.let { bearerAuth(it) }
        }
        decode(response.status.value, response.status.isSuccess(), response.bodyAsText(), method)
    }

    private suspend fun get(url: String, parameters: Map<String, String> = emptyMap()): JsonObject = withHttp { http ->
        require(Url(url).protocol.name == "https") { "HTTPS is required." }
        val response = http.get(url) { url { parameters.forEach { (k, v) -> this.parameters.append(k, v) } } }
        decode(response.status.value, response.status.isSuccess(), response.bodyAsText(), Url(url).encodedPath.substringAfterLast('/'))
    }

    private fun decode(status: Int, success: Boolean, body: String, method: String): JsonObject {
        // Never include response bodies in exceptions: authentication responses contain secrets.
        val value = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        if (!success) {
            val error = (value?.get("error") as? JsonPrimitive)?.contentOrNull
                ?.takeIf { it in setOf("AuthFactorTokenRequired", "AuthenticationRequired", "InvalidToken", "ExpiredToken", "InvalidSwap", "RecordNotFound") }
                ?: "RequestFailed"
            // Never log authentication response messages or bodies, which may contain secrets.
            Logger.w { "teal.fm $method failed ($status, $error)" }
            throw TealApiException(error, status)
        }
        return value ?: throw IOException("PDS returned an invalid response.")
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true }

        internal fun httpsOrigin(value: String): String {
            val url = Url(value)
            require(url.protocol.name == "https" && url.user.isNullOrEmpty() && url.password.isNullOrEmpty() &&
                url.parameters.isEmpty() && url.fragment.isEmpty() && url.encodedPath in listOf("", "/") &&
                url.host.isNotBlank() && url.host != "localhost") { "The PDS must have an HTTPS origin URL." }
            return value.trimEnd('/')
        }
    }
}

internal fun JsonObject.string(key: String) = (this[key] as? JsonPrimitive)
    ?.takeIf { it.isString && it.content.isNotBlank() }?.content
    ?: throw IOException("PDS response is missing or has an invalid $key.")
