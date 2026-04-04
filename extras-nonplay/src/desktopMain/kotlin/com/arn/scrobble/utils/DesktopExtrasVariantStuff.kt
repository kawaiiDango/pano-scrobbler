package com.arn.scrobble.utils

import com.arn.scrobble.VariantStuffInterface
import com.arn.scrobble.billing.BaseBillingRepository
import com.arn.scrobble.billing.BillingRepository
import com.arn.scrobble.review.BaseReviewPrompter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class DesktopExtrasVariantStuff(
    scope: CoroutineScope,
    receipt: Flow<Pair<String?, String?>>,
    lastCheckTime: Flow<Long>,
    setLastcheckTime: suspend (Long) -> Unit,
    setReceipt: suspend (String?, String?) -> Unit,

    httpPost: suspend (url: String, body: String) -> String,
    deviceIdentifier: () -> String,
    openInBrowser: (url: String) -> Unit,
) : VariantStuffInterface {
    override val billingRepository: BaseBillingRepository = BillingRepository(
        scope,
        receipt,
        lastCheckTime,
        setLastcheckTime,
        setReceipt,

        httpPost,
        deviceIdentifier,
        openInBrowser,
        null
    )
    override val reviewPrompter: BaseReviewPrompter = BaseReviewPrompter()
    override val githubApiUrl: String =
        "https://api.github.com/repos/kawaiiDango/pano-scrobbler/releases/latest"
    override val hasForegroundServiceSpecialUse = false
}