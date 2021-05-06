package com.arn.scrobble

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import de.umass.lastfm.Track

class MainNotifierViewModel(application: Application): AndroidViewModel(application) {

    val drawerData by lazy { MutableLiveData(DrawerData.loadFromPref(application)) }
    val editData by lazy { MutableLiveData<Track>() }

}