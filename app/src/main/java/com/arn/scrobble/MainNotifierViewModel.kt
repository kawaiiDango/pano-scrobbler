package com.arn.scrobble

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.Track

class MainNotifierViewModel(application: Application) : AndroidViewModel(application) {

    val drawerData by lazy { MutableLiveData(DrawerData.loadFromPref(application)) }
    val editData by lazy { LiveEvent<Track>() }
    var backButtonEnabled = true
    val trackBundleLd = MutableLiveData<Bundle>()

}