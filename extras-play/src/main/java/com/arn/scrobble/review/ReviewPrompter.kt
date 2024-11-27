package com.arn.scrobble.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ReviewPrompter(
    private val activity: Any?,
    lastReviewPromptTime: Long?,
    override val setReviewPromptTime: suspend (Long?) -> Unit,

    ) : BaseReviewPrompter(activity, lastReviewPromptTime, setReviewPromptTime) {
    override fun launchReviewFlow() {
        activity as Activity
        val manager = ReviewManagerFactory.create(activity)

        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                GlobalScope.launch {
                    setReviewPromptTime(System.currentTimeMillis())
                }
                manager.launchReviewFlow(activity, task.result)
            } else {
                task.exception?.printStackTrace()
            }
        }

    }
}
