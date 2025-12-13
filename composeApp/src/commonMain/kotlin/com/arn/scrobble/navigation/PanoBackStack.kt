package com.arn.scrobble.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.savedstate.serialization.SavedStateConfiguration

@Composable
fun rememberPanoNavBackStack(
    configuration: SavedStateConfiguration,
    vararg elements: PanoRoute,
): NavBackStack<PanoRoute> {
    require(configuration.serializersModule != SavedStateConfiguration.DEFAULT.serializersModule) {
        "You must pass a `SavedStateConfiguration.serializersModule` configured to handle " +
                "`NavKey` open polymorphism. Define it with: `polymorphic(NavKey::class) { ... }`"
    }
    return rememberSerializable(
        configuration = configuration,
        serializer = NavBackStackSerializer<PanoRoute>(),
    ) {
        NavBackStack(*elements)
    }
}