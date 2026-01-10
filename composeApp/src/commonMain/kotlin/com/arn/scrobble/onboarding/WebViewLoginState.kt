package com.arn.scrobble.onboarding

sealed interface WebViewLoginState {
    data object None : WebViewLoginState
    data object Unavailable : WebViewLoginState
    data object Processing : WebViewLoginState
    data object Success : WebViewLoginState
    data class Failed(val errorMsg: String) : WebViewLoginState
}