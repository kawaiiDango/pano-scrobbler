package com.arn.scrobble.utils

import com.arn.scrobble.VariantStuffInterface
import com.arn.scrobble.billing.BaseBillingRepository
import com.arn.scrobble.review.BaseReviewPrompter

class ExtrasVariantStuff(
    override val billingRepository: BaseBillingRepository
) : VariantStuffInterface {
    override val reviewPrompter = BaseReviewPrompter()
    override val githubApiUrl: String? = platformGithubApiUrl
    override val hasForegroundServiceSpecialUse = platformHasForegroundServiceSpecialUse
}

internal expect val platformGithubApiUrl: String?
internal expect val platformHasForegroundServiceSpecialUse: Boolean
