package com.arn.scrobble.ui

import android.view.View
import de.umass.lastfm.MusicEntry

interface MusicEntryItemClickListener {
    fun onItemClick(view: View, entry: MusicEntry)
}