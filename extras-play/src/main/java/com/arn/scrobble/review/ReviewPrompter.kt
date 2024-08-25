package com.arn.scrobble.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

class ReviewPrompter(
    private val activity: Activity,
    lastReviewPromptTime: Long?,
    override val setReviewPromptTime: (Long?) -> Unit

) : BaseReviewPrompter(activity, lastReviewPromptTime, setReviewPromptTime) {
    override fun launchReviewFlow() {
        val manager = ReviewManagerFactory.create(activity)

        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                setReviewPromptTime(System.currentTimeMillis())
                manager.launchReviewFlow(activity, task.result)
            } else {
                task.exception?.printStackTrace()
            }
        }

    }
}
