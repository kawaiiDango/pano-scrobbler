package com.arn.scrobble

import android.app.Activity
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager

class ReviewPrompter(private val activity: Activity) {
    private val prefs by lazy { MainPrefs(activity) }

    fun showIfNeeded(): Boolean {
        if (prefs.lastReviewPromptTime == null) {
            prefs.lastReviewPromptTime = System.currentTimeMillis()
        }

        val shouldShowPrompt = System.currentTimeMillis() - prefs.lastReviewPromptTime!! >= INTERVAL

        if (shouldShowPrompt)
            launchReviewFlow()

        return shouldShowPrompt
    }

    private fun launchReviewFlow() {
        val manager = if (BuildConfig.DEBUG)
            FakeReviewManager(activity)
        else
            ReviewManagerFactory.create(activity)

        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (BuildConfig.DEBUG)
                    activity.toast("We got the ReviewInfo object")
                prefs.lastReviewPromptTime = System.currentTimeMillis()

                manager.launchReviewFlow(activity, task.result)
            } else {
                task.exception?.printStackTrace()
            }
        }

    }

    fun resetData() {
        prefs.lastReviewPromptTime = null
    }

    companion object {
        private const val INTERVAL = 10 * 24 * 60 * 60 * 1000 // n days
    }
}
