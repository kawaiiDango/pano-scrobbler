package com.arn.scrobble

import com.arn.scrobble.billing.BaseBillingRepository
import com.arn.scrobble.crashreporter.BaseCrashReporter
import com.arn.scrobble.crashreporter.CrashReporter
import com.arn.scrobble.review.BaseReviewPrompter
import com.arn.scrobble.review.ReviewPrompter

class ExtrasVariantStuff(
    override val billingRepository: BaseBillingRepository
) : VariantStuffInterface {
    override val crashReporter: BaseCrashReporter = CrashReporter
    override val reviewPrompter: BaseReviewPrompter = ReviewPrompter
    override val githubApiUrl: String? = null
    override val hasForegroundServiceSpecialUse = false
}