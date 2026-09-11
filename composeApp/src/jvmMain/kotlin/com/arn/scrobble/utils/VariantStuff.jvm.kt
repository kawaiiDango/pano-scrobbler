package com.arn.scrobble.utils

import com.arn.scrobble.VariantStuffInterface
import com.arn.scrobble.billing.BaseBillingRepository
import com.arn.scrobble.billing.BillingRepository
import com.arn.scrobble.review.BaseReviewPrompter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

actual val VariantStuff = object : VariantStuffInterface {
    override val billingRepository: BaseBillingRepository = BillingRepository(
        context = null,
        scope = Stuff.appScope,
        lastCheckTime = flow {
            emitAll(PlatformStuff.mainPrefs.data.map { it.lastLicenseCheckTime }
                .distinctUntilChanged())
        },
        setLastCheckTime = { time ->
            PlatformStuff.mainPrefs.updateData { it.copy(lastLicenseCheckTime = time) }
        },
        receipt = flow { emitAll(Stuff.receiptFlow) },
        setReceipt = Stuff::setReceipt,
        httpPost = Stuff::httpPost,
        deviceIdentifier = PlatformStuff::getDeviceIdentifier,
        openInBrowser = PlatformStuff::openInBrowser,
    )

    override val reviewPrompter: BaseReviewPrompter = BaseReviewPrompter(
        lastCheckTime = flowOf(-1L),
        setLastCheckTime = { }
    )

    override val githubApiUrl: String =
        "https://api.github.com/repos/kawaiiDango/pano-scrobbler/releases/latest"
}