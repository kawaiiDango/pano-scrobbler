package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R

class TranslatorsVM : ViewModel() {
    // read each line from the raw resource file and collect them into a list
    val translators = PlatformStuff.application
        .resources
        .openRawResource(R.raw.crowdin_members)
        .bufferedReader()
        .use { it.readLines() }
}