package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import com.arn.scrobble.charts.ChartsVM

class InfoExtraVM: ViewModel() {
    val artistsVM by lazy { ChartsVM() }
    val albumsVM by lazy { ChartsVM() }
    val tracksVM by lazy { ChartsVM() }
}