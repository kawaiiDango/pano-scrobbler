package com.arn.scrobble

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import com.arn.scrobble.Stuff.focusOnTv
import com.arn.scrobble.pref.MainPrefs
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

class AppRater(
    private val context: Context,
    private val prefs: MainPrefs,
) {

    fun appLaunched(): Boolean {
        if (prefs.dontAskForRating) {
            return false
        }

        if (prefs.firstLaunchTime == null) {
            prefs.firstLaunchTime = System.currentTimeMillis()
        }

        val shouldShowPrompt = prefs.scrobbleCount >= MIN_SCROBBLES &&
                System.currentTimeMillis() >= prefs.firstLaunchTime!! + MIN_DAYS * 24 * 60 * 60 * 1000

        if (shouldShowPrompt)
            showRateSnackbar()

        return shouldShowPrompt
    }

    private fun rateNow() {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Stuff.MARKET_URL)))
        } catch (activityNotFoundException1: ActivityNotFoundException) {
            Timber.tag("AppRater").e("Market Intent not found")
        }
    }

    fun showRateSnackbar() {
        val coordinatorLayout = (context as Activity).findViewById<View>(R.id.frame)
        Snackbar.make(coordinatorLayout, R.string.rate_msg, Snackbar.LENGTH_INDEFINITE)
            .setAction("\uD83C\uDF1F " + context.getString(R.string.rate_action)) {
                rateNow()
                prefs.dontAskForRating = true
            }
            .focusOnTv()
            .show()
    }

    fun resetData() {
        prefs.firstLaunchTime = null
        prefs.dontAskForRating = false
        prefs.scrobbleCount = 0
    }

    companion object {
        private const val MIN_DAYS = 6
        private const val MIN_SCROBBLES = 20

        fun incrementScrobbleCount(pref: MainPrefs) {
            if (pref.scrobbleCount < MIN_SCROBBLES)
                pref.scrobbleCount++
        }
    }
}
