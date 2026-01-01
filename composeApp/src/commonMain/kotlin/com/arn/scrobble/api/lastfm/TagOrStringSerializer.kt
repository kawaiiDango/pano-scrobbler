package com.arn.scrobble.api.lastfm

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject

object TagsOrStringSerializer : KSerializer<Tags> {
    override val descriptor = Tags.serializer().descriptor
    private val delegate = Tags.serializer()

    override fun serialize(encoder: Encoder, value: Tags) {
        encoder.encodeSerializableValue(delegate, value)
    }

    override fun deserialize(decoder: Decoder): Tags {
        if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            return if (element is JsonObject) {
                decoder.json.decodeFromJsonElement(delegate, element)
            } else {
                Tags(emptyList())
            }
        }

        // Non-JSON: try normal decoding
        return decoder.decodeSerializableValue(delegate)
    }
}