package com.arn.scrobble.utils

import com.arn.scrobble.VariantStuffInterface
import com.arn.scrobble.billing.BaseBillingRepository
import com.arn.scrobble.review.BaseReviewPrompter
import com.arn.scrobble.review.ReviewPrompter

class ExtrasVariantStuff(
    override val billingRepository: BaseBillingRepository
) : VariantStuffInterface {
    override val reviewPrompter: BaseReviewPrompter = ReviewPrompter
    override val githubApiUrl: String? = null
    override val hasForegroundServiceSpecialUse = false
}