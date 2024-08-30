package com.arn.scrobble.review

import android.app.Activity

abstract class BaseReviewPrompter(
    private val activity: Activity,
    private val lastReviewPromptTime: Long?,
    protected open val setReviewPromptTime: (Long?) -> Unit
) {
    fun showIfNeeded(): Boolean {
        if (lastReviewPromptTime == null) {
            setReviewPromptTime(System.currentTimeMillis())
            return false
        }

        val shouldShowPrompt = System.currentTimeMillis() - lastReviewPromptTime >= INTERVAL

        if (shouldShowPrompt)
            launchReviewFlow()

        return shouldShowPrompt
    }

    abstract fun launchReviewFlow()

    fun resetData() {
        setReviewPromptTime(null)
    }

    companion object {
        private const val INTERVAL = 10 * 24 * 60 * 60 * 1000 // n days
    }
}