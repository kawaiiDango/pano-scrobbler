package com.arn.scrobble.themes

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import java.util.function.Consumer


@Composable
actual fun isSystemInDarkThemeNative(): State<Boolean> {
    val isDark = isSystemInDarkTheme()

    return produceState(isDark, isDark) {
        value = isDark
    }
}

@RequiresApi(31)
actual fun getDynamicColorScheme(context: Any?, dark: Boolean): ColorScheme {
    val context = context as Context
    return if (dark) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
}

@Composable
actual fun AddAdditionalProviders(content: @Composable () -> Unit) {
    content()
}

actual fun setupWindowBlurListener(activity: Any?, onBlurChanged: (Boolean) -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val activity = activity as? Activity ?: return

    val decorView = activity.window.decorView
    val onBlurChangedConsumer = Consumer(onBlurChanged)

    fun addListener() {
        activity.windowManager.addCrossWindowBlurEnabledListener(onBlurChangedConsumer)
    }

    fun removeListener() {
        activity.windowManager.removeCrossWindowBlurEnabledListener(onBlurChangedConsumer)
    }

    if (decorView.isAttachedToWindow) {
        addListener()
    }

    decorView.addOnAttachStateChangeListener(
        object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = addListener()
            override fun onViewDetachedFromWindow(v: View) = removeListener()
        }
    )
}