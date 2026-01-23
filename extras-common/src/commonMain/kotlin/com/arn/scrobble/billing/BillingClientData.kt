package com.arn.scrobble.billing

import kotlinx.coroutines.flow.Flow

data class BillingClientData(
    val proProductId: String,
    val appName: String,
    val httpPost: suspend (url: String, body: String) -> String,
    val deviceIdentifier: () -> String,
    val lastCheckTime: Flow<Long>,
    val setLastcheckTime: suspend (Long) -> Unit,
    val receipt: Flow<Pair<String?, String?>>,
    val setReceipt: suspend (String?, String?) -> Unit,
)