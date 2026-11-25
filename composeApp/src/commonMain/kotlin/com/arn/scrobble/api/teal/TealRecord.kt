package com.arn.scrobble.api.teal

import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import kotlinx.serialization.json.*
import java.security.MessageDigest
import kotlin.time.Instant

internal const val TEAL_PLAY = "fm.teal.feed.play"
internal const val TEAL_STATUS = "fm.teal.actor.status"

private val mbidPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
internal fun mbidUri(value: String?): String? = value?.takeIf { mbidPattern.matches(it) }?.let { "mbid:${it.lowercase()}" }

internal class TealInvalidListenException(message: String) : IllegalArgumentException(message)

internal fun JsonObject.hasValidTealOptionalTrackFields(): Boolean {
    val release = this["releaseName"]
    val duration = this["duration"]
    return (release == null || (release as? JsonPrimitive)?.isString == true) &&
        (duration == null || (duration as? JsonPrimitive)?.let {
            !it.isString && it.longOrNull?.let { seconds -> seconds in 0..Long.MAX_VALUE / 1000 } == true
        } == true)
}

internal fun ScrobbleData.validateTealFields() {
    if (track.isBlank() || track.toByteArray(Charsets.UTF_8).size > 256)
        throw TealInvalidListenException("The track name is empty or too long for teal.fm.")
    if (artist.toByteArray(Charsets.UTF_8).size > 256 || (album?.toByteArray(Charsets.UTF_8)?.size ?: 0) > 256)
        throw TealInvalidListenException("The artist or album name is too long for teal.fm.")
}

internal fun ScrobbleData.toTealRecord(agent: String): JsonObject = buildJsonObject {
    validateTealFields()
    put("\$type", TEAL_PLAY)
    put("trackName", track)
    putJsonArray("artists") {
        if (artist.isNotBlank()) add(buildJsonObject {
            put("artistName", artist)
            mbidUri(extraTags[ListenBrainz.MUSICBRAINZ_ARTISTID])?.let { put("artistMbId", it) }
        })
    }
    album?.takeIf { it.isNotBlank() }?.let { put("releaseName", it) }
    mbidUri(extraTags[ListenBrainz.MUSICBRAINZ_TRACKID])?.let { put("recordingMbId", it) }
    mbidUri(extraTags[ListenBrainz.MUSICBRAINZ_RELEASETRACKID])?.let { put("trackMbId", it) }
    mbidUri(extraTags[ListenBrainz.MUSICBRAINZ_ALBUMID])?.let { put("releaseMbId", it) }
    duration?.takeIf { it > 0 }?.let { put("duration", it / 1000) }
    put("playedTime", Instant.fromEpochMilliseconds(timestamp).toString())
    put("submissionClientAgent", agent)
}

/** Stable across retries, process restarts and batches. The TID time is the listen's start time. */
internal fun ScrobbleData.tealRecordKey(): String {
    if (timestamp !in 1..8_000_000_000_000L) throw TealInvalidListenException("Invalid listen timestamp.")
    validateTealFields()
    val digest = MessageDigest.getInstance("SHA-256")
        .digest("$artist\u0000$track\u0000${appId.orEmpty()}".toByteArray(Charsets.UTF_8))
    val clock = ((digest[0].toInt() and 0xff) shl 2) or (digest[1].toInt() and 3)
    var value = (timestamp * 1000 shl 10) or clock.toLong()
    val alphabet = "234567abcdefghijklmnopqrstuvwxyz"
    return CharArray(13).also { chars ->
        for (i in chars.lastIndex downTo 0) {
            chars[i] = alphabet[(value and 31).toInt()]
            value = value ushr 5
        }
    }.concatToString()
}
