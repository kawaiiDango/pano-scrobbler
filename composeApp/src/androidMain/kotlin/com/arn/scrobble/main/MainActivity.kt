package com.arn.scrobble.main

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.core.util.Consumer
import androidx.navigation.compose.rememberNavController
import com.arn.scrobble.themes.AppTheme
import com.arn.scrobble.themes.DayNightMode
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.applyAndroidLocaleLegacy

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        // this is required for SystemBarStyle.auto to correctly detect night mode
        when (Stuff.mainPrefsInitialValue.themeDayNight) {
            DayNightMode.SYSTEM -> {
                enableEdgeToEdge()
            }

            DayNightMode.DARK, DayNightMode.LIGHT -> {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) {
                        Stuff.mainPrefsInitialValue.themeDayNight == DayNightMode.DARK
                    },
                    navigationBarStyle = SystemBarStyle.auto(DefaultLightScrim, DefaultDarkScrim) {
                        Stuff.mainPrefsInitialValue.themeDayNight == DayNightMode.DARK
                    }
                )
            }
        }

        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val navController = rememberNavController()

                DisposableEffect(navController) {
                    val consumer = Consumer<Intent> {
                        navController.handleDeepLink(it)
                    }
                    addOnNewIntentListener(consumer)
                    onDispose {
                        removeOnNewIntentListener(consumer)
                    }
                }

                PanoAppContent(navController)
            }
        }

//        navDeepLink<PanoRoute.MusicEntryInfo>(
//            basePath = Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.MusicEntryInfo::class.simpleName,
//            typeMap = mapOf(
//                typeOf<UserCached>() to serializableType<UserCached>(),
//                typeOf<Artist?>() to serializableType<Artist?>(),
//                typeOf<Album?>() to serializableType<Album?>(),
//                typeOf<Track?>() to serializableType<Track?>(),
//            )
//        ).let {
//            Logger.i { "Deep link: ${it.uriPattern}" }
//        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase ?: return)
        applyAndroidLocaleLegacy()
    }

}
// copied from EdgeToEdge.kt

// The light scrim color used in the platform API 29+
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/com/android/internal/policy/DecorView.java;drc=6ef0f022c333385dba2c294e35b8de544455bf19;l=142
private val DefaultLightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

// The dark scrim color used in the platform.
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/res/color/system_bar_background_semi_transparent.xml
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/remote_color_resources_res/values/colors.xml;l=67
private val DefaultDarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
