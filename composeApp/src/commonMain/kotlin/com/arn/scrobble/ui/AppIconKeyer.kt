package com.arn.scrobble.ui

import coil3.key.Keyer
import coil3.request.Options

class AppIconKeyer : Keyer<PackageName> {
    override fun key(data: PackageName, options: Options) = "package:" + data.packageName
}