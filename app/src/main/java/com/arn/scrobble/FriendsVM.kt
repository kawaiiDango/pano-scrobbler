package com.arn.scrobble

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Track
import de.umass.lastfm.User


class FriendsVM(app: Application): AndroidViewModel(app) {
    private val friends = MutableLiveData<PaginatedResult<User>>()
    private val track = MutableLiveData<Pair<String,PaginatedResult<Track>>>()

    private var page = 1

    fun loadFriendsList(page: Int, reload: Boolean): MutableLiveData<PaginatedResult<User>> {
        this.page = page
        if (reload)
            LFMRequester(Stuff.GET_FRIENDS, page.toString()).asAsyncTask(getApplication(), friends)
        return friends
    }
    fun loadFriendsRecents(user: String?): MutableLiveData<Pair<String,PaginatedResult<Track>>> {
        if (user != null)
            LFMRequester(Stuff.GET_FRIENDS_RECENTS, user).asAsyncTask(getApplication(), track)
        return track
    }
}