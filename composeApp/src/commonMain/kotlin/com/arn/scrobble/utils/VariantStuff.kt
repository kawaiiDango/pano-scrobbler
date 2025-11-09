package com.arn.scrobble.utils

import com.arn.scrobble.ExtrasPropsInterface
import com.arn.scrobble.billing.BaseBillingRepository
import com.arn.scrobble.crashreporter.BaseCrashReporter
import com.arn.scrobble.review.BaseReviewPrompter

object VariantStuff {
    lateinit var crashReporter: BaseCrashReporter
    lateinit var billingRepository: BaseBillingRepository
    lateinit var reviewPrompter: BaseReviewPrompter
    lateinit var extrasProps: ExtrasPropsInterface
}