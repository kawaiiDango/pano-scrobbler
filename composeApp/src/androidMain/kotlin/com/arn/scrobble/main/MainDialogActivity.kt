package com.arn.scrobble.main

import android.os.Build
import android.os.Bundle

class MainDialogActivity : MainActivity() {
    override val isDialogActivity = true

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0, 0)
        } else {
            overridePendingTransition(0, 0)
        }
        super.onCreate(savedInstanceState)
    }
}