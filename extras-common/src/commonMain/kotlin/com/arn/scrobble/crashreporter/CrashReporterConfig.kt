package com.arn.scrobble.crashreporter

import java.io.File


object CrashReporterConfig {
    private var disabledFile: File? = null
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

    fun init(disabledFile: File) {
        this.disabledFile = disabledFile
    }
}