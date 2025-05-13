package com.arn.scrobble.imageloader

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory


actual fun ImageLoader.Builder.additionalOptions(): ImageLoader.Builder {
    return this
}

actual fun ComponentRegistry.Builder.additionalComponents(): ComponentRegistry.Builder {
    add(OkHttpNetworkFetcherFactory()) // proguard removes it for some reason
    return this
}