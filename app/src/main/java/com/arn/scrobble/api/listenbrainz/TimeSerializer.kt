package com.arn.scrobble.api.listenbrainz

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// this deserializes as a long (millis) but serializes as (seconds)
class TimeSerializer : KSerializer<Long> {
    override val descriptor = Long.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Long) {
        val seconds = value / 1000
        encoder.encodeLong(seconds)
    }

    override fun deserialize(decoder: Decoder): Long {
        return decoder.decodeLong() * 1000
    }
}