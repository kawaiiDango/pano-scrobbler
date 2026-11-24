package com.arn.scrobble.api.atproto

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.lastfm.ScrobbleData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.MessageDigest
import java.time.Instant

internal class AtprotoPlayRejected(message: String) : Exception(message)
internal class AtprotoReconnect : Exception("Sign in again to send AT Protocol plays")

/** A TID from the original play time and metadata, not the send time. Identical plays at the same
 * millisecond are indistinguishable. Keep the inputs stable across app versions and Pending retries. */
internal fun ScrobbleData.atprotoRecordKey(): String {
    if (timestamp !in 1..((Long.MAX_VALUE ushr 10) / 1_000))
        throw AtprotoPlayRejected("Invalid AT Protocol play timestamp")
    val identity = Json.encodeToString(
        listOf(artist, track, album, albumArtist, appId, duration?.toString(), trackNumber?.toString(), isrc())
    )
    val hash = MessageDigest.getInstance("SHA-256").digest(identity.encodeToByteArray())
    val clock = ((hash[0].toInt() and 0xff) shl 2) or (hash[1].toInt() and 3)
    val tid = (timestamp * 1_000 shl 10) or clock.toLong()
    val alphabet = "234567abcdefghijklmnopqrstuvwxyz"
    return CharArray(13) { alphabet[((tid ushr (60 - it * 5)) and 31).toInt()] }.concatToString()
}

private fun ScrobbleData.isrc() = extraTags.entries.firstOrNull { it.key.equals("isrc", ignoreCase = true) }
    ?.value?.takeIf { it.isNotBlank() && it.encodeToByteArray().size <= 32 }

/** Keep this mapping stable: retries reconstruct the same record from the persisted ScrobbleData. */
internal fun ScrobbleData.atprotoPlay(type: AccountType): JsonObject {
    val started = Instant.ofEpochMilli(timestamp).toString()
    fun String?.required(field: String, maxBytes: Int): String =
        this?.takeIf { it.isNotBlank() && it.encodeToByteArray().size <= maxBytes }
            ?: throw AtprotoPlayRejected("Missing or invalid $field for ${type.playCollection}")

    return buildJsonObject {
        put("\$type", type.playCollection)
        when (type) {
            AccountType.TEAL -> {
                put("trackName", track.required("title", 256))
                put("artists", buildJsonArray { add(buildJsonObject { put("artistName", artist.required("artist", 256)) }) })
                put("playedTime", started)
                album?.takeIf { it.isNotBlank() && it.encodeToByteArray().size <= 256 }?.let { put("releaseName", it) }
                duration?.div(1000)?.takeIf { it > 0 }?.let { put("duration", it) }
            }
            AccountType.ROCKSKY -> {
                put("title", track.required("title", 512))
                put("artist", artist.required("artist", 256))
                put("album", album.required("album", 256))
                put("albumArtist", albumArtist.required("album artist", 256))
                put("duration", duration?.takeIf { it > 0 }
                    ?: throw AtprotoPlayRejected("Rocksky requires a known track duration"))
                put("createdAt", started)
                trackNumber?.takeIf { it > 0 }?.let { put("trackNumber", it) }
            }
            else -> error("Not an AT Protocol scrobble destination")
        }
        isrc()?.let { put("isrc", it) }
    }
}
