package com.arn.scrobble.billing

enum class LicenseState {
    PENDING,
    VALID,
    REJECTED,
    NO_LICENSE,
    MAX_DEVICES_REACHED,
}