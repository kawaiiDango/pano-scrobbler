package com.arn.scrobble.api.lastfm

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object ArtistOrStringSerializer : KSerializer<Artist> {

    override val descriptor = Artist.serializer().descriptor
    private val delegate = Artist.serializer()

    override fun serialize(encoder: Encoder, value: Artist) {
        encoder.encodeSerializableValue(delegate, value)
    }

    override fun deserialize(decoder: Decoder): Artist {
        return if (decoder is JsonDecoder) {
            when (val element = decoder.decodeJsonElement()) {
                is JsonObject -> decoder.json.decodeFromJsonElement(Artist.serializer(), element)
                is JsonPrimitive -> Artist(element.content)
                else -> throw SerializationException("Expected JsonObject or JsonPrimitive")
            }
        } else {
            // Non-JSON: try normal decoding
            decoder.decodeSerializableValue(delegate)
        }
    }
}