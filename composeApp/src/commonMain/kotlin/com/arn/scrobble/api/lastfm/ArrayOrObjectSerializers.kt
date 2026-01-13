package com.arn.scrobble.api.lastfm

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject


sealed class ArrayOrObjectSerializer<T>(elementSerializer: KSerializer<T>) :
    KSerializer<List<T>> {
    private val listSerializer = ListSerializer(elementSerializer)

    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun deserialize(decoder: Decoder): List<T> {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            val transformedElement = when (element) {
                is JsonArray -> element
                is JsonObject -> JsonArray(listOf(element))
                else -> JsonArray(emptyList())
            }
            decoder.json.decodeFromJsonElement(listSerializer, transformedElement)
        } else {
            listSerializer.deserialize(decoder)
        }
    }

    override fun serialize(encoder: Encoder, value: List<T>) {
        listSerializer.serialize(encoder, value)
    }
}

object ArrayOrObjectTagSerializer : ArrayOrObjectSerializer<Tag>(Tag.serializer())
object ArrayOrObjectArtistSerializer : ArrayOrObjectSerializer<Artist>(Artist.serializer())
object ArrayOrObjectTrackSerializer : ArrayOrObjectSerializer<Track>(Track.serializer())
object ArrayOrObjectAlbumSerializer : ArrayOrObjectSerializer<Album>(Album.serializer())
object ArrayOrObjectScrobbleDetailsSerializer :
    ArrayOrObjectSerializer<ScrobbleDetails>(ScrobbleDetails.serializer())