package com.arn.scrobble.api.lastfm

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class LastfmUnixTimestampSerializer : KSerializer<Long?> {
    override val descriptor = Long.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Long?) {
        val seconds = value?.div(1000)
        seconds?.let { encoder.encodeLong(it) }
    }

    override fun deserialize(decoder: Decoder): Long? {
        // JSON: accept { "unixtime": "..."} or { "uts": "..." }
        if (decoder is JsonDecoder) {
            return when (val elem = decoder.decodeJsonElement()) {
                is JsonObject -> {
                    val obj = elem.jsonObject
                    val key = if ("unixtime" in obj) "unixtime" else "uts"
                    obj[key]?.jsonPrimitive?.content?.toLongOrNull()?.times(1000)
                }

                is JsonPrimitive -> elem.longOrNull?.times(1000)

                else -> null
            }
        }

        // Non-JSON
        decoder.decodeLong().let { seconds ->
            return seconds.times(1000)
        }
    }
}