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

    override fun serialize(encoder: Encoder, value: Artist) {
        encoder.encodeSerializableValue(Artist.serializer(), value)
    }

    override fun deserialize(decoder: Decoder): Artist {
        decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")
        return when (val element = decoder.decodeJsonElement()) {
            is JsonObject -> decoder.json.decodeFromJsonElement(Artist.serializer(), element)
            is JsonPrimitive -> Artist(element.content)
            else -> throw SerializationException("Expected JsonObject or JsonPrimitive")
        }
    }
}