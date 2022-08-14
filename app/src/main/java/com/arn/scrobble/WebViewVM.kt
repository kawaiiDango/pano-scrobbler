package com.arn.scrobble

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class WebViewVM(app: Application) : AndroidViewModel(app) {
    val callbackProcessedLd = MutableLiveData<Boolean>()
}
