package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import com.arn.scrobble.R
import com.arn.scrobble.main.App
import com.arn.scrobble.utils.Stuff
import kotlinx.serialization.json.decodeFromStream

class TranslatorsVM : ViewModel() {
    val translators =
        Stuff.myJson.decodeFromStream<List<String>>(App.context.resources.openRawResource(R.raw.crowdin_members))
}