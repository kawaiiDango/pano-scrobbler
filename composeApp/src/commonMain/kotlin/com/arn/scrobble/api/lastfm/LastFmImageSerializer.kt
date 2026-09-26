package com.arn.scrobble.api.lastfm

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject

object LastFmImageSerializer : KSerializer<ImagesUrls?> {
    private val delegate = ImagesUrls.serializer().nullable

    override val descriptor: SerialDescriptor = delegate.descriptor
    private val toWebpExtensions = arrayOf(".jpg", ".jpeg", ".png")
    private val sizePriority = listOf("extralarge", "large")

    override fun serialize(encoder: Encoder, value: ImagesUrls?) {
        delegate.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): ImagesUrls? {
        // any non-JSON format: stored values, untouched.
        if (decoder !is JsonDecoder) return delegate.deserialize(decoder)

        val json = decoder.json
        return when (val element = decoder.decodeJsonElement()) {
            // Last.fm API response: convert once.
            is JsonArray -> fromLastFm(
                json.decodeFromJsonElement(
                    ListSerializer(RawLastFmImage.serializer()),
                    element,
                )
            )
            // Our own persisted JSON: use as-is.
            is JsonObject -> json.decodeFromJsonElement(ImagesUrls.serializer(), element)
            // JsonNull, or Last.fm sending an empty string.
            else -> null
        }
    }

    private fun fromLastFm(raw: List<RawLastFmImage>): ImagesUrls? {
        var source = sizePriority.firstNotNullOfOrNull { size ->
            raw.firstOrNull { it.size == size && it.url.isNotBlank() }?.url
        } ?: return null

        if (toWebpExtensions.any { source.endsWith(it) })
            source = source.substringBeforeLast(".") + ".webp"

        val large = source.replace("300x300", "600x600")
        return ImagesUrls(source, large)
    }

    @Serializable
    private data class RawLastFmImage(
        val size: String,
        @SerialName("#text")
        val url: String,
    )
}