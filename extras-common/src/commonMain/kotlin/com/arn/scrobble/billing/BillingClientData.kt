package com.arn.scrobble.billing

import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class BillingClientData(
    val proProductId: String,
    val appName: String,
    val publicKeyBase64: String,
    val httpClient: HttpClient,
    val serverUrl: String,
    val deviceIdentifier: String,
    val lastcheckTime: Flow<Long>,
    val setLastcheckTime: suspend (Long) -> Unit,
    val receipt: StateFlow<Pair<String?, String?>>,
    val setReceipt: suspend (String?, String?) -> Unit,
)