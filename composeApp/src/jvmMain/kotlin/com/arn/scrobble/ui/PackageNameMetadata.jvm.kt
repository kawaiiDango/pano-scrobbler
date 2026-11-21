package com.arn.scrobble.ui

import com.arn.scrobble.utils.PlatformStuff

actual object PackageNameMetadata {
    actual val PackageName.englishLabel: String? get() = PlatformStuff.loadApplicationLabel(this.packageName)
    actual val PackageName.version: String? get() = null
}