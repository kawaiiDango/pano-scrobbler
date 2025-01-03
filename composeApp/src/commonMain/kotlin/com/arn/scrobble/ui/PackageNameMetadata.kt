package com.arn.scrobble.ui

expect object PackageNameMetadata {
    val PackageName.englishLabel: String?
    val PackageName.version: String?
}