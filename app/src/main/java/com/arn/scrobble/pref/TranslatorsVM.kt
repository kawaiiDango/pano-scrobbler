package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import com.arn.scrobble.R
import com.arn.scrobble.main.App

class TranslatorsVM : ViewModel() {
    // read each line from the raw resource file and collect them into a list
    val translators = App.context
        .resources
        .openRawResource(R.raw.crowdin_members)
        .bufferedReader()
        .use { it.readLines() }
}