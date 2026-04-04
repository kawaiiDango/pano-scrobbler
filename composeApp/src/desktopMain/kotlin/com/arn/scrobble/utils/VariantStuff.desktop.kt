package com.arn.scrobble.utils

import com.arn.scrobble.VariantStuffInterface
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

actual val VariantStuff: VariantStuffInterface = DesktopExtrasVariantStuff(
    scope = Stuff.appScope,
    lastCheckTime = flow { emitAll(PlatformStuff.mainPrefs.data.map { it.lastLicenseCheckTime }) },
    setLastcheckTime = { time ->
        PlatformStuff.mainPrefs.updateData { it.copy(lastLicenseCheckTime = time) }
    },
    receipt = flow { emitAll(Stuff.receiptFlow) },
    setReceipt = Stuff::setReceipt,
    httpPost = Stuff::httpPost,
    deviceIdentifier = PlatformStuff::getDeviceIdentifier,
    openInBrowser = PlatformStuff::openInBrowser,
)