package com.arn.scrobble.api.listenbrainz

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

// this deserializes as a long (millis) but serializes as (seconds)
class TimeNullableSerializer : KSerializer<Long?> {
    override val descriptor = Long.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Long?) {
        val seconds = value?.div(1000) ?: return
        encoder.encodeLong(seconds)
    }

    override fun deserialize(decoder: Decoder): Long? {
        val jsonDecoder =
            decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")
        val elem = jsonDecoder.decodeJsonElement()
        return elem.jsonPrimitive.longOrNull?.times(1000)
    }
}