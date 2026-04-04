package com.arn.scrobble.review

import kotlin.time.Duration.Companion.days

open class BaseReviewPrompter {
    open suspend fun showIfNeeded(
        activity: Any?,
        lastReviewPromptTime: suspend () -> Long?,
        setReviewPromptTime: suspend (Long?) -> Unit,
    ): Boolean {
        val lastReviewPromptTime = lastReviewPromptTime()
        if (lastReviewPromptTime == null) {
            setReviewPromptTime(System.currentTimeMillis())
            return false
        }

        val shouldShowPrompt =
            System.currentTimeMillis() - lastReviewPromptTime >= 60.days.inWholeMilliseconds

        return shouldShowPrompt
    }
}