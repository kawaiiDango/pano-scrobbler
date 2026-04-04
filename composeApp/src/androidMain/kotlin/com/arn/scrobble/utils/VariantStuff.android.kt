package com.arn.scrobble.utils

import com.arn.scrobble.VariantStuffInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

private val lastLicenseCheckTimeFile
    get() =
        AndroidStuff.applicationContext.noBackupFilesDir
            .resolve("last_license_check_time.txt")
actual val VariantStuff: VariantStuffInterface = AndroidExtrasVariantStuff(
    scope = Stuff.appScope,
    lastCheckTime = flow {
        val t = withContext(Dispatchers.IO) {
            lastLicenseCheckTimeFile
                .takeIf { it.exists() }
                ?.readText()
                ?.toLongOrNull()
        } ?: -1L

        emit(t)
    },
    setLastcheckTime = { time ->
        withContext(Dispatchers.IO) {
            lastLicenseCheckTimeFile
                .writeText(time.toString())
        }
    },
    receipt = flow { emitAll(Stuff.receiptFlow) },
    setReceipt = Stuff::setReceipt,
    httpPost = Stuff::httpPost,
    deviceIdentifier = PlatformStuff::getDeviceIdentifier,
    openInBrowser = PlatformStuff::openInBrowser,
    context = AndroidStuff.applicationContext
)