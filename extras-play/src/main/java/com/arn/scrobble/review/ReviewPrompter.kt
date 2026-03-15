package com.arn.scrobble.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal object ReviewPrompter : BaseReviewPrompter() {
    override suspend fun showIfNeeded(
        activity: Any?,
        lastReviewPromptTime: suspend () -> Long?,
        setReviewPromptTime: suspend (Long?) -> Unit
    ): Boolean {
        if (activity !is Activity || true) return false // todo disable for now

        val show = super.showIfNeeded(activity, lastReviewPromptTime, setReviewPromptTime)

        if (show) {
            val manager = ReviewManagerFactory.create(activity)

            coroutineScope {
                manager.requestReviewFlow().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        launch {
                            setReviewPromptTime(System.currentTimeMillis())
                        }
                        manager.launchReviewFlow(activity, task.result)
                    } else {
                        task.exception?.printStackTrace()
                    }
                }
            }
        }

        return show
    }
}
