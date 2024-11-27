package com.arn.scrobble.review


class ReviewPrompter(
    activity: Any?,
    lastReviewPromptTime: Long?,
    setReviewPromptTime: suspend (Long?) -> Unit,

    ) : BaseReviewPrompter(activity, lastReviewPromptTime, setReviewPromptTime) {

    override fun launchReviewFlow() {

    }
}