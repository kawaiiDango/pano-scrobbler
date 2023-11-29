package com.arn.scrobble.api.lastfm

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject


//object TagsOrStringSerializer : JsonTransformingSerializer<Tags>(serializer()) {
//    override fun transformDeserialize(element: JsonElement): JsonElement {
//        return when (element) {
//            is JsonObject -> element
//            else -> JsonNull
//        }
//    }
//}

object TagsOrStringSerializer : KSerializer<Tags> {
    override val descriptor = Tags.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Tags) {
    }

    override fun deserialize(decoder: Decoder): Tags {
        decoder as? JsonDecoder ?: throw SerializationException("Expected JsonDecoder")
        return when (val element = decoder.decodeJsonElement()) {
            is JsonObject -> decoder.json.decodeFromJsonElement(Tags.serializer(), element)
            else -> Tags(emptyList())
        }
    }
}