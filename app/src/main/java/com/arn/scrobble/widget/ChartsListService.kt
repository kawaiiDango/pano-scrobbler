package com.arn.scrobble.widget

import android.content.Intent
import android.widget.RemoteViewsService

class ChartsListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent) =
        ChartsListRemoteViewsFactory(applicationContext, intent)
}