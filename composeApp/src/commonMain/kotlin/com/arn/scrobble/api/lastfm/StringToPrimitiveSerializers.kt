package com.arn.scrobble.api.lastfm

import kotlinx.serialization.KSerializer
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
        if (decoder is JsonDecoder) {
            return when (val element = decoder.decodeJsonElement()) {
                is JsonPrimitive -> element.contentOrNull?.toIntOrNull() ?: 0
                else -> 0
            }
        }

        // Non-JSON
        return decoder.decodeInt()
    }
}

class StringOrLongSerializer : KSerializer<Long> {
    override val descriptor = Long.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }

    override fun deserialize(decoder: Decoder): Long {
        if (decoder is JsonDecoder) {
            return when (val element = decoder.decodeJsonElement()) {
                is JsonPrimitive -> element.contentOrNull?.toLongOrNull() ?: 0L
                else -> 0L
            }
        }

        // Non-JSON
        return decoder.decodeLong()
    }
}

class StringSecsToMsSerializer : KSerializer<Long> {
    override val descriptor = Int.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value.div(1000))
    }

    override fun deserialize(decoder: Decoder): Long {
        if (decoder is JsonDecoder) {
            return when (val element = decoder.decodeJsonElement()) {
                is JsonPrimitive -> element.contentOrNull?.toLongOrNull()?.times(1000) ?: 0
                else -> 0
            }
        }

        // Non-JSON
        return decoder.decodeLong() * 1000
    }
}

class StringOrBoolSerializer : KSerializer<Boolean> {
    override val descriptor = Int.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }

    override fun deserialize(decoder: Decoder): Boolean {
        if (decoder is JsonDecoder) {
            return when (val element = decoder.decodeJsonElement()) {
                is JsonPrimitive -> {
                    val str = element.contentOrNull
                    str == "1" || str == "true"
                }

                else -> false
            }
        }

        // Non-JSON
        return decoder.decodeBoolean()
    }
}

class StringOrFloatSerializer : KSerializer<Float> {
    override val descriptor = Float.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Float) {
        encoder.encodeFloat(value)
    }

    override fun deserialize(decoder: Decoder): Float {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                // contentOrNull works for both numeric primitives and quoted strings.
                return element.contentOrNull?.toFloatOrNull() ?: 0f
            }
            return 0f
        }

        // Non-JSON
        return decoder.decodeFloat()
    }
}