package com.arn.scrobble.utils

import android.content.Context
import com.arn.scrobble.VariantStuffInterface
import com.arn.scrobble.billing.BaseBillingRepository
import com.arn.scrobble.billing.BillingRepository
import com.arn.scrobble.review.BaseReviewPrompter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class AndroidExtrasVariantStuff(
    scope: CoroutineScope,
    receipt: Flow<Pair<String?, String?>>,
    lastCheckTime: Flow<Long>,
    setLastcheckTime: suspend (Long) -> Unit,
    setReceipt: suspend (String?, String?) -> Unit,

    httpPost: suspend (url: String, body: String) -> String,
    deviceIdentifier: () -> String,
    openInBrowser: (url: String) -> Unit,
    context: Context
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
        context
    )
    override val reviewPrompter: BaseReviewPrompter = AndroidVariantStuffProps.reviewPrompter
    override val githubApiUrl: String? = null
    override val hasForegroundServiceSpecialUse =
        AndroidVariantStuffProps.hasForegroundServiceSpecialUse
}