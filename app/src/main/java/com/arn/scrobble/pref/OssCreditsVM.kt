package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import com.arn.scrobble.PlatformStuff
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext

class OssCreditsVM : ViewModel() {
    val libraries = Libs.Builder().withContext(PlatformStuff.application).build().libraries
}