package com.arn.scrobble.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
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

@Serializable(with = ApiErrorDeserializer::class)
data class ApiErrorResponse(
    val code: Int,
    val message: String,
)


object ApiErrorDeserializer : KSerializer<ApiErrorResponse> {

    override val descriptor = buildClassSerialDescriptor("LastFmErrorResponse") {
        element<Int>("code")
        element<String>("message")
    }

    override fun serialize(encoder: Encoder, value: ApiErrorResponse) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeIntElement(descriptor, 0, value.code)
        composite.encodeStringElement(descriptor, 1, value.message)
        composite.endStructure(descriptor)
    }

//    val variant1 = "{\"error\":{\"#text\":\"Invalid resource specified\",\"code\":\"7\"}}" // gnufm
//    val variant2 =
//        "{\"message\":\"Invalid API key - You must be granted a valid key by last.fm\",\"error\":10}" // lastfm
//    val variant3 = "{\"code\": 200, \"error\": \"Invalid Method\"}" // listenbrainz
//    val variant4 = "{\"error\": \"Invalid token\"}" // maloja
//    val variant5 =  "{\"error\":{\"message\":\"Stuff\",\"status\": 404}}" // Spotify

    override fun deserialize(decoder: Decoder): ApiErrorResponse {
        val input = decoder as? JsonDecoder
            ?: error("Can be deserialized only by JSON")
        val apiErrorResponse = when (val jsonElement = input.decodeJsonElement()) {
            is JsonObject -> {
                when (val errorObject = jsonElement["error"]) {
                    is JsonObject -> {
                        val code =
                            (errorObject["code"] ?: errorObject["status"])?.jsonPrimitive?.int ?: 0
                        val message =
                            (errorObject["message"] ?: errorObject["#text"])?.jsonPrimitive?.content
                                ?: ""

                        ApiErrorResponse(
                            code = code,
                            message = message
                        )
                    }

                    is JsonPrimitive -> {
                        if (errorObject.jsonPrimitive.isString)
                            ApiErrorResponse(
                                code = jsonElement["code"]?.jsonPrimitive?.int ?: 0,
                                message = errorObject.jsonPrimitive.content
                            )
                        else
                            ApiErrorResponse(
                                code = errorObject.jsonPrimitive.int,
                                message = jsonElement["message"]?.jsonPrimitive?.content ?: ""
                            )
                    }

                    else -> throw SerializationException("Unknown JSON structure")
                }
            }

            else -> throw SerializationException("Unknown JSON structure")
        }

        if (apiErrorResponse.code == 17) // lastfm error
            return apiErrorResponse.copy(message = "This profile is private")
        return apiErrorResponse
    }

}