package com.arn.scrobble.friends

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.arn.scrobble.LFMRequester
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import de.umass.lastfm.User


class FriendsVM(app: Application): AndroidViewModel(app) {
    val friends = mutableListOf<User>()
    val receiver = MutableLiveData<PaginatedResult<User>>()
    val track = MutableLiveData<Pair<String,PaginatedResult<Track>>>()
    val paletteColorsCache = mutableMapOf<String, Int>()
    var username: String? = null
    var page = 1
    var totalPages = 1
    var sorted = false

    fun loadFriendsList(page: Int) {
        this.page = page
        LFMRequester(getApplication()).getFriends(page, username).asAsyncTask(receiver)
    }
    fun loadFriendsRecents(user: String) {
        LFMRequester(getApplication()).getFriendsRecents(user).asAsyncTask(track)
    }
}