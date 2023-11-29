package com.arn.scrobble.api.lastfm

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.serializer


object ArrayOrObjectTagSerializer : JsonTransformingSerializer<List<Tag>>(serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonArray -> element
            is JsonObject -> JsonArray(listOf(element))
            else -> JsonArray(emptyList())
        }
    }
}

object ArrayOrObjectArtistSerializer : JsonTransformingSerializer<List<Artist>>(serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonArray -> element
            is JsonObject -> JsonArray(listOf(element))
            else -> JsonArray(emptyList())
        }
    }
}

object ArrayOrObjectTrackSerializer : JsonTransformingSerializer<List<Track>>(serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonArray -> element
            is JsonObject -> JsonArray(listOf(element))
            else -> JsonArray(emptyList())
        }
    }
}

object ArrayOrObjectAlbumSerializer : JsonTransformingSerializer<List<Album>>(serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return when (element) {
            is JsonArray -> element
            is JsonObject -> JsonArray(listOf(element))
            else -> JsonArray(emptyList())
        }
    }
}
