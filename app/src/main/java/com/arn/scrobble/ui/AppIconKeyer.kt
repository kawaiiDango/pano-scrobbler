package com.arn.scrobble.ui

import coil.key.Keyer
import coil.request.Options

class AppIconKeyer : Keyer<PackageName> {

    override fun key(data: PackageName, options: Options) = "package:" + data.packageName

}