package com.arn.scrobble.crashreporter

import java.io.File


open class BaseCrashReporter {
    protected var disabledFile: File? = null
    val isAvailable get() = disabledFile != null

    var isEnabled: Boolean
        get() = disabledFile?.exists() != true
        set(value) {
            disabledFile?.let { disabledFile ->
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

    open fun init(
        disabledFile: File? = null,
        keysMap: Map<String, String> = emptyMap(),
    ) {
    }
}