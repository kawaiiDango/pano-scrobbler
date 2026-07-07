package com.arn.scrobble.utils

import com.arn.scrobble.VariantStuffInterface
import com.arn.scrobble.billing.BaseBillingRepository
import com.arn.scrobble.billing.BillingRepository
import com.arn.scrobble.review.BaseReviewPrompter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DesktopExtrasVariantStuff(
    scope: CoroutineScope,
    receipt: Flow<Pair<String?, String?>>,
    lastCheckTime: Flow<Long>,
    setLastCheckTime: suspend (Long) -> Unit,
    setReceipt: suspend (String?, String?) -> Unit,

    httpPost: suspend (url: String, body: String) -> String,
    deviceIdentifier: () -> String,
    openInBrowser: (url: String) -> Unit,
) : VariantStuffInterface {
    override val billingRepository: BaseBillingRepository = BillingRepository(
        scope,
        receipt,
        lastCheckTime,
        setLastCheckTime,
        setReceipt,

        httpPost,
        deviceIdentifier,
        openInBrowser,
        null
    )
    override val reviewPrompter: BaseReviewPrompter = BaseReviewPrompter(
        lastCheckTime = flowOf(-1L),
        setLastCheckTime = { }
    )
    override val githubApiUrl: String =
        "https://api.github.com/repos/kawaiiDango/pano-scrobbler/releases/latest"
}