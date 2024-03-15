package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import com.arn.scrobble.main.App
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext

class LicensesVM : ViewModel() {
    val libraries = Libs.Builder().withContext(App.context).build().libraries
}