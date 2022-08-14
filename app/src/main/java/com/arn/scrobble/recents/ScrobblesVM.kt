package com.arn.scrobble.recents

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.R
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import kotlinx.coroutines.sync.Mutex

class ScrobblesVM(application: Application) : AndroidViewModel(application) {
    val tracksReceiver by lazy { LiveEvent<PaginatedResult<Track>>() }
    val tracks by lazy { mutableListOf<Track>() }
    val pendingScrobbles by lazy { mutableListOf<PendingScrobble>() }
    val pendingLoves by lazy { mutableListOf<PendingLove>() }

    private val mutex = Mutex()

    var username: String? = null
    var page = 1
    var totalPages = 1
    private var loadedCached = false
    var selectedPos = 1
    var toTime: Long? = null
    val selectedChipId = R.id.recents_chip

    fun loadRecents(page: Int) {
        this.page = page
        LFMRequester(getApplication(), viewModelScope, tracksReceiver)
            .getRecents(
                page,
                username,
                cached = !loadedCached,
                to = toTime ?: -1L,
                includeNowPlaying = toTime == null,
                doDeltaIndex = page == 1 && toTime == null && username == null // todo: && theres no unsubmitted pending scrobbles
            )
        loadedCached = true
    }
}