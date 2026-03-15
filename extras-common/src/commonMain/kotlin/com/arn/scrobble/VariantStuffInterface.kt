package com.arn.scrobble

import com.arn.scrobble.billing.BaseBillingRepository
import com.arn.scrobble.crashreporter.BaseCrashReporter
import com.arn.scrobble.review.BaseReviewPrompter


interface VariantStuffInterface {
    val billingRepository: BaseBillingRepository
    val crashReporter: BaseCrashReporter
    val reviewPrompter: BaseReviewPrompter
    val githubApiUrl: String?
    val hasForegroundServiceSpecialUse: Boolean
}