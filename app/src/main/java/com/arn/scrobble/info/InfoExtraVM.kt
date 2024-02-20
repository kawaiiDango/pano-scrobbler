package com.arn.scrobble.info

import androidx.lifecycle.ViewModel

class InfoExtraVM : ViewModel() {
    val artistsVM by lazy { InfoExtraFullVM() }
    val albumsVM by lazy { InfoExtraFullVM() }
    val tracksVM by lazy { InfoExtraFullVM() }
}