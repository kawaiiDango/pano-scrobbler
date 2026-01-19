package com.arn.scrobble.billing

import androidx.compose.runtime.compositionLocalOf

val LocalLicenseValidState = compositionLocalOf<Boolean> {
    error("No LicenseValidState provided")
}
