package com.arn.scrobble.ui

import android.view.View
import com.arn.scrobble.api.lastfm.MusicEntry

interface MusicEntryItemClickListener {
    fun onItemClick(view: View, entry: MusicEntry)
}