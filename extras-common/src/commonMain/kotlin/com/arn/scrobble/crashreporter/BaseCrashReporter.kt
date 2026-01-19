package com.arn.scrobble.crashreporter

import java.io.File


abstract class BaseCrashReporter(
    private val disabledFile: File?,
) {
    val isAvailable = disabledFile != null

    var isEnabled: Boolean
        get() = disabledFile?.exists() != true
        set(value) {
            if (disabledFile != null) {
                if (value) {
                    if (disabledFile.exists()) {
                        disabledFile.delete()
                    }
                } else {
                    if (!disabledFile.exists()) {
                        disabledFile.createNewFile()
                    }
                }
            }
        }

    open fun config(
        keysMap: Map<String, String> = emptyMap(),
    ) {
    }
}