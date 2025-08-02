package com.arn.scrobble.api.lastfm

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull


class StringOrIntSerializer : KSerializer<Int> {
    override val descriptor = Int.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")

        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> element.contentOrNull?.toIntOrNull() ?: 0
            else -> 0
        }
    }
}

class StringOrLongSerializer : KSerializer<Long> {
    override val descriptor = Long.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }

    override fun deserialize(decoder: Decoder): Long {
        decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")

        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> element.contentOrNull?.toLongOrNull() ?: 0L
            else -> 0L
        }
    }
}

class StringSecsToMsSerializer : KSerializer<Long> {
    override val descriptor = Int.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value.div(1000))
    }

    override fun deserialize(decoder: Decoder): Long {
        decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")

        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> element.contentOrNull?.toLongOrNull()?.times(1000) ?: 0
            else -> 0
        }
    }
}

class StringOrBoolSerializer : KSerializer<Boolean> {
    override val descriptor = Int.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }

    override fun deserialize(decoder: Decoder): Boolean {
        decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")

        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                val str = element.contentOrNull
                str == "1" || str == "true"
            }

            else -> false
        }
    }
}

class StringOrFloatSerializer : KSerializer<Float> {
    override val descriptor = Float.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Float) {
        encoder.encodeFloat(value)
    }

    override fun deserialize(decoder: Decoder): Float {
        decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")

        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> element.contentOrNull?.toFloatOrNull() ?: 0f
            else -> 0f
        }
    }
}