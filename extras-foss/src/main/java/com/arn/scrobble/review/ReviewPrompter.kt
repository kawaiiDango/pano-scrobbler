package com.arn.scrobble.review

import android.app.Activity


class ReviewPrompter(
    activity: Activity,
    lastReviewPromptTime: Long?,
    setReviewPromptTime: (Long?) -> Unit

) : BaseReviewPrompter(activity, lastReviewPromptTime, setReviewPromptTime) {

    override fun launchReviewFlow() {

    }
}