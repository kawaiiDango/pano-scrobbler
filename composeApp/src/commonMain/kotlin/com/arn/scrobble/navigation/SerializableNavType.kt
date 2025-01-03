package com.arn.scrobble.navigation

import androidx.core.bundle.Bundle
import androidx.navigation.NavType
import io.ktor.http.decodeURLPart
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified T : Any?> serializableType(
    json: Json = Json,
) = object : NavType<T>(isNullableAllowed = null is T) {
    override fun get(bundle: Bundle, key: String): T? = bundle.getString(key)?.let {
        json.decodeFromString<T>(it.decodeURLPart())
    }

    // Navigation takes care of decoding the string
    // before passing it to parseValue()
    override fun parseValue(value: String): T = json.decodeFromString(value)

    // Serialized values must always be Uri encoded
    override fun serializeAsValue(value: T): String = json.encodeToString(value).encodeURLPathPart()

    override fun put(bundle: Bundle, key: String, value: T) {
        bundle.putString(key, serializeAsValue(value))
    }
}