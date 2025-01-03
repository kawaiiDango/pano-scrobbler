package com.arn.scrobble.imageloader

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.request.allowHardware


actual fun ImageLoader.Builder.additionalOptions(): ImageLoader.Builder {
    allowHardware(false)
    return this
}

actual fun ComponentRegistry.Builder.additionalComponents(): ComponentRegistry.Builder {
    add(AppIconKeyer())
    add(AppIconFetcher.Factory())
    return this
}