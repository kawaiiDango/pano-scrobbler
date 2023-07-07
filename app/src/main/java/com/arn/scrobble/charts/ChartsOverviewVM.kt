package com.arn.scrobble.charts

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.App
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.R
import com.arn.scrobble.friends.UserSerializable
import com.arn.scrobble.scrobbleable.Scrobblables
import com.hadilq.liveevent.LiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ChartsOverviewVM : ViewModel() {
    val artistsVM by lazy { ChartsVM() }
    val albumsVM by lazy { ChartsVM() }
    val tracksVM by lazy { ChartsVM() }

    val listeningActivity by lazy { MutableLiveData<Map<TimePeriod, Int>>() }
    val tagCloud by lazy { MutableLiveData<Map<String, Float>>() }
    val tagCloudError by lazy { LiveEvent<Throwable>() }
    val tagCloudRefresh by lazy { LiveEvent<Unit>() }
    var listeningActivityRequested = false
    var tagCloudRequested = false
    val tagCloudProgressLd by lazy { MutableLiveData<Double>() }
    val listeningActivityHeader =
        MutableLiveData(App.context.getString(R.string.listening_activity))
    private var lastListeningActivityJob: Job? = null
    var tagCloudBitmap: Pair<Int, Bitmap?>? = null
    private var lastTagCloudTask: LFMRequester? = null
    private var lastListeningActivityTask: LFMRequester? = null


    fun loadListeningActivity(user: UserSerializable, timePeriod: TimePeriod) {
        lastListeningActivityJob?.cancel()
        lastListeningActivityJob =
            viewModelScope.launch(Dispatchers.IO + LFMRequester.ExceptionNotifier()) {
                listeningActivity.postValue(
                    Scrobblables.current?.getListeningActivity(
                        timePeriod, user
                    )
                )
            }
    }

    fun loadTagCloud() {
        if (!artistsVM.hasLoaded())
            return

        tagCloudRequested = true
        lastTagCloudTask?.cancel()
        lastTagCloudTask =
            LFMRequester(
                viewModelScope, liveData = tagCloud,
                errorLiveData = tagCloudError
            ).apply {
                getTagCloud(artistsVM.chartsData, tagCloudProgressLd)
            }
    }

    fun resetRequestedState() {
        listeningActivityRequested = false
        tagCloudRequested = false
        lastTagCloudTask?.cancel()
        lastListeningActivityTask?.cancel()
    }

}