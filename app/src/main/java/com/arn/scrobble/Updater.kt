package com.arn.scrobble

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.arn.scrobble.Stuff.isChannelEnabled
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.UiUtils.addAction
import com.arn.scrobble.ui.UiUtils.focusOnTv
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.IOException

class Updater(
    private val context: Context,
) {
    private val prefs = App.prefs
    private val json = Json { ignoreUnknownKeys = true }

    // check if play store exists
    private val hasPlayStore by lazy {
        context
            .packageManager
            .getLaunchIntentForPackage("com.android.vending") != null
    }

    private suspend fun checkGithubForUpdates(
        force: Boolean = false,
        onError: ((Exception) -> Unit)? = null,
        onUpdateAvailable: (String, String, String) -> Unit
    ) {
        if (hasPlayStore) {
            prefs.checkForUpdates = null

            if (!BuildConfig.DEBUG)
                return
        } else if (prefs.checkForUpdates == null || force) {
            prefs.checkForUpdates = true
        }


        val now = System.currentTimeMillis()
        if (prefs.checkForUpdates != true
            || !((now - (prefs.lastUpdateCheckTime ?: -1)) > UPDATE_CHECK_INTERVAL || force)
        )
            return

        withContext(Dispatchers.IO) {
            val request = Request(githubApiUrl.toHttpUrl())

            try {
                LFMRequester.okHttpClient
                    .newCall(request)
                    .execute()
                    .use { response ->

                        if (response.isSuccessful) {
                            val body = response.body.byteStream()
                            val releases = json.decodeFromStream<GithubReleases>(body)
                            val upstreamVersionCode =
                                Integer.parseInt(releases.tag_name.replace(".", ""))

                            prefs.lastUpdateCheckTime = now

                            if (BuildConfig.DEBUG) {
                                Stuff.log("Upstream version code: $upstreamVersionCode")
                            }

                            if (upstreamVersionCode > BuildConfig.VERSION_CODE) {
                                val firstAsset = releases.assets.find { it.name.endsWith(".apk") }

                                if (firstAsset != null) {

                                    val downloadUrl = firstAsset.browser_download_url

                                    withContext(Dispatchers.Main) {
                                        onUpdateAvailable(
                                            releases.name,
                                            releases.body,
                                            downloadUrl
                                        )
                                    }
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                onError?.invoke(IOException("Response code: ${response.code}"))
                            }
                        }

                        Unit
                    }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError?.invoke(e)
                }
            }
        }
    }

    // from activity
    fun withSnackbar() {
        val mainActivity = context as MainActivity
        mainActivity.lifecycleScope.launch {
            checkGithubForUpdates(
                onError = {
                    if (BuildConfig.DEBUG)
                        it.printStackTrace()
                },
                onUpdateAvailable = { versionName, changelog, downloadUrl ->
                    val coordinatorLayout = mainActivity.findViewById<View>(R.id.coordinator)
                    Snackbar.make(
                        coordinatorLayout,
                        mainActivity.getString(R.string.update_available, versionName),
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(mainActivity.getString(R.string.changelog)) {
                            MaterialAlertDialogBuilder(mainActivity)
                                .setTitle(versionName)
                                .setMessage(changelog)
                                .setPositiveButton(R.string.download) { _, _ ->
                                    Stuff.openInBrowser(downloadUrl)
                                }
                                .show()
                        }
                        .focusOnTv()
                        .addAction(R.layout.button_snackbar_extra, R.string.download) {
                            Stuff.openInBrowser(downloadUrl)
                        }
                        .show()
                }
            )
        }
    }

    // from job
    fun withNotification() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!notificationManager.isChannelEnabled(
                prefs.sharedPreferences,
                MainPrefs.CHANNEL_NOTI_UPDATE
            )
        )
            return

        GlobalScope.launch {

            checkGithubForUpdates(
                force = true,
                onError = {
                    if (BuildConfig.DEBUG)
                        it.printStackTrace()
                },
                onUpdateAvailable = { versionName, changelog, downloadUrl ->
                    // initialize notification channel
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel(
                            MainPrefs.CHANNEL_NOTI_UPDATE,
                            context.getString(R.string.update_available, ""),
                            NotificationManager.IMPORTANCE_LOW
                        )
                    }

                    // create notification
                    val builder = NotificationCompat.Builder(context, MainPrefs.CHANNEL_NOTI_UPDATE)
                        .setSmallIcon(R.drawable.vd_noti)
                        .setContentTitle(context.getString(R.string.update_available, versionName))
                        .setContentText(context.getString(R.string.download))
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setAutoCancel(true)
                        .setContentIntent(
                            PendingIntent.getActivity(
                                context,
                                100,
                                Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)),
                                Stuff.updateCurrentOrImmutable
                            )
                        )
                        .setAutoCancel(true)
                        .build()

                    // show notification
                    notificationManager.notify(MainPrefs.CHANNEL_NOTI_UPDATE, 100, builder)
                }
            )
        }
    }
}

@kotlinx.serialization.Serializable
data class GithubReleases(
    val tag_name: String,
    val name: String,
    val body: String,
    val published_at: String,
    val html_url: String,
    val assets: List<GithubReleaseAsset>
)

@kotlinx.serialization.Serializable
data class GithubReleaseAsset(
    val name: String,
    val browser_download_url: String
)

const val githubApiUrl = "https://api.github.com/repos/kawaiiDango/pscrobbler/releases/latest"
const val UPDATE_CHECK_INTERVAL = 60 * 60 * 1000L // 1 hour
