package com.arn.scrobble.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.core.util.Consumer
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import co.touchlab.kermit.Logger
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.serializableType
import com.arn.scrobble.themes.AppTheme
import com.arn.scrobble.utils.LocaleUtils.setLocaleCompat
import com.arn.scrobble.utils.Stuff
import kotlin.reflect.typeOf

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge()
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
        setLocaleCompat()
    }

}