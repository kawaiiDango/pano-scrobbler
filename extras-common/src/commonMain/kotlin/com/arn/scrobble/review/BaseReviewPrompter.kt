package com.arn.scrobble.review

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class BaseReviewPrompter {
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

        val shouldShowPrompt = System.currentTimeMillis() - lastReviewPromptTime >= INTERVAL

        return shouldShowPrompt
    }

    companion object {
        private const val INTERVAL = 10 * 24 * 60 * 60 * 1000 // n days
    }
}