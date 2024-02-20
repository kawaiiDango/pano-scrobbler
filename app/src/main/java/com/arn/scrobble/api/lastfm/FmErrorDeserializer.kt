package com.arn.scrobble.api.lastfm

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

//@Serializable
//data class LastFmErrorResponse(
//    val code: Int,
//    val message: String
//)

object FmErrorDeserializer : KSerializer<FmErrorResponse> {

    override val descriptor = buildClassSerialDescriptor("LastFmErrorResponse") {
        element<Int>("code")
        element<String>("message")
    }

    override fun serialize(encoder: Encoder, value: FmErrorResponse) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeIntElement(descriptor, 0, value.code)
        composite.encodeStringElement(descriptor, 1, value.message)
        composite.endStructure(descriptor)
    }

//    val variant1 = "{\"error\":{\"#text\":\"Invalid resource specified\",\"code\":\"7\"}}" // gnufm
//    val variant2 =
//        "{\"message\":\"Invalid API key - You must be granted a valid key by last.fm\",\"error\":10}" // lastfm
//    val variant3 = "{\"code\": 200, \"error\": \"Invalid Method\"}" // listenbrainz

    override fun deserialize(decoder: Decoder): FmErrorResponse {
        val input = decoder as? JsonDecoder
            ?: error("Can be deserialized only by JSON")
        return when (val jsonElement = input.decodeJsonElement()) {
            is JsonObject -> {
                when (val errorObject = jsonElement["error"]) {
                    is JsonObject -> FmErrorResponse(
                        code = errorObject["code"]?.jsonPrimitive?.int ?: 0,
                        message = errorObject["#text"]?.jsonPrimitive?.content ?: ""
                    )

                    is JsonPrimitive -> {
                        if (errorObject.jsonPrimitive.isString)
                            FmErrorResponse(
                                code = jsonElement["code"]?.jsonPrimitive?.int ?: 0,
                                message = errorObject.jsonPrimitive.content
                            )
                        else
                            FmErrorResponse(
                                code = errorObject.jsonPrimitive.int,
                                message = jsonElement["message"]?.jsonPrimitive?.content ?: ""
                            )
                    }

                    else -> throw SerializationException("Unknown JSON structure")
                }
            }

            else -> throw SerializationException("Unknown JSON structure")
        }
    }

}