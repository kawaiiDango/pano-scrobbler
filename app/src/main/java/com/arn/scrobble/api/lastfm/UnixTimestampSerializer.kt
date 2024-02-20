package com.arn.scrobble.api.lastfm

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class UnixTimestampSerializer : KSerializer<Int?> {
    override val descriptor = Int.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Int?) {
    }

    override fun deserialize(decoder: Decoder): Int? {
        val jsonDecoder =
            decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")
        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        val key = if ("unixtime" in jsonObject) "unixtime" else "uts"
        return jsonObject[key]?.jsonPrimitive?.content?.toIntOrNull()
    }
}