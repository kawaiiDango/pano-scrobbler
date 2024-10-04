package com.arn.scrobble.imageloader

import coil3.key.Keyer
import coil3.request.Options
import com.arn.scrobble.ui.PackageName

class AppIconKeyer : Keyer<PackageName> {
    override fun key(data: PackageName, options: Options) = "package:" + data.packageName
}