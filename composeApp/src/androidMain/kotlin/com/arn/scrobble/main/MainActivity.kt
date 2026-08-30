package com.arn.scrobble.main

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.arn.scrobble.R
import com.arn.scrobble.navigation.LocalActivityRestoredFlag
import com.arn.scrobble.themes.AppTheme
import com.arn.scrobble.themes.LocalThemeAttributes
import com.arn.scrobble.utils.AndroidStuff.prolongSplashScreen
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.applyAndroidLocaleLegacy

open class MainActivity : ComponentActivity() {
    protected open val isDialogActivity = false
    private val isTranslucentMarkerFile by lazy { noBackupFilesDir.resolve("is_translucent") }

    override fun onCreate(savedInstanceState: Bundle?) {
        val restoredState = savedInstanceState
            ?: intent.getBundleExtra(MANUAL_SAVED_STATE)

        super.onCreate(restoredState)

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        var initDone = false
        prolongSplashScreen { initDone }

        setContent {
            AppTheme(
                onInitDone = { initDone = true }
            ) {
                val themeAttributes = LocalThemeAttributes.current
                val density = LocalDensity.current

                LaunchedEffect(themeAttributes.isDark) {
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !themeAttributes.isDark
                        isAppearanceLightNavigationBars = !themeAttributes.isDark
                    }

                    if (Build.VERSION.SDK_INT in 26..27) {
                        // fix always light navigation bar on Oreo
                        val defaultLightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
                        // The dark scrim color used in the platform.
                        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/res/color/system_bar_background_semi_transparent.xml
                        // https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/remote_color_resources_res/values/colors.xml;l=67
                        val defaultDarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
                        window.navigationBarColor =
                            if (themeAttributes.isDark)
                                defaultDarkScrim
                            else
                                defaultLightScrim
                    }
                }

                LaunchedEffect(themeAttributes.isTranslucent, themeAttributes.blurMainWindow) {
                    val markerFileExists = isTranslucentMarkerFile.exists()
                    val translucencyChanged = themeAttributes.isTranslucent != markerFileExists

                    if (translucencyChanged) {
                        if (themeAttributes.isTranslucent) {
                            isTranslucentMarkerFile.createNewFile()
                        } else {
                            isTranslucentMarkerFile.delete()
                        }
                        myRecreate()
                        // don't touch blur flags on a window that's being torn down
                        // the new activity instance will run this same effect and apply blur there
                        return@LaunchedEffect
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && PlatformStuff.supportsBlur) {
                        val isBlurEnabled =
                            (window.attributes.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND) != 0

                        if (themeAttributes.blurMainWindow && !isBlurEnabled) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                            val attributes = window.attributes
                            attributes.blurBehindRadius =
                                with(density) { Stuff.BLUR_FROSTED_RADIUS_DP.dp.roundToPx() }
                            attributes.dimAmount = 0.01f  // near-zero, but satisfies the compositor
                            window.attributes = attributes
                        } else if (!themeAttributes.blurMainWindow && isBlurEnabled) {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                        }
                    }
                }

                CompositionLocalProvider(LocalActivityRestoredFlag provides (restoredState != null)) {
                    PanoAppContent(
                        onCloseLastDialog = if (isDialogActivity) {
                            onBackPressedDispatcher::onBackPressed
                        } else null
                    )
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.applyAndroidLocaleLegacy() ?: return)
    }

    override fun onApplyThemeResource(theme: Resources.Theme, resid: Int, first: Boolean) {
        super.onApplyThemeResource(theme, resid, first)

        if (!isDialogActivity && isTranslucentMarkerFile.exists()) {
            theme.applyStyle(R.style.Patch_Wallpaper, true)
        }
    }

    private fun myRecreate() {
        val stateBundle = Bundle()
        onSaveInstanceState(stateBundle)

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            putExtra(MANUAL_SAVED_STATE, stateBundle)
        }
        finish()
        startActivity(intent)
    }

    companion object {
        private const val MANUAL_SAVED_STATE = "manual_saved_state"
    }
}