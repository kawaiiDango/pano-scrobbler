package com.arn.scrobble.billing

enum class LicenseState {
    UNKNOWN,
    NO_LICENSE,
    VALID,
}

enum class LicenseError {
    PENDING,
    REJECTED,
    MAX_DEVICES_REACHED,
    NETWORK_ERROR,
}