package com.arn.scrobble

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WebViewVM : ViewModel() {
    val callbackProcessedLd = MutableLiveData<Boolean>()
}
