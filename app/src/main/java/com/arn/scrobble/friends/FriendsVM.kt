package com.arn.scrobble.friends

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.mapConcurrently
import com.arn.scrobble.friends.UserSerializable.Companion.toUserSerializable
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.recents.PaletteColors
import com.arn.scrobble.ui.SectionWithHeader
import com.arn.scrobble.ui.SectionedVirtualList
import com.hadilq.liveevent.LiveEvent
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Session
import de.umass.lastfm.Track
import de.umass.lastfm.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope


class FriendsVM(app: Application) : AndroidViewModel(app) {
    val friendsFiltered = mutableListOf<UserSerializable>()
    private val friendsOrig = mutableListOf<UserSerializable>()
    private val prefs by lazy { MainPrefs(getApplication()) }
    val pinnedFriends = mutableListOf<UserSerializable>()
    private val pinnedUsernames = hashSetOf<String>()
    val lastPlayedTracksMap = mutableMapOf<String, Track?>()
    val playCountsMap = mutableMapOf<String, Int>()
    val urlToPaletteMap = mutableMapOf<String, PaletteColors>()
    val friendsReceiver = LiveEvent<PaginatedResult<User>>()
    val tracksReceiver = LiveEvent<Pair<String, PaginatedResult<Track>>>()
    var username: String? = null
    var page = 1
    var totalPages = 1
    var sorted = false
    var hasLoaded = false

    val sectionedList by lazy {

        if (username == null)
            prefs.pinnedFriendsJson
                .sortedBy { it.order }
                .forEach { addPin(it, save = false) }

        SectionedVirtualList().apply {
            addSection(
                SectionWithHeader(
                    FriendsAdapter.FriendType.PINNED,
                    pinnedFriends,
                )
            )
            addSection(
                SectionWithHeader(
                    FriendsAdapter.FriendType.FRIEND,
                    friendsFiltered,
                )
            )
        }
    }

    fun loadFriendsList(page: Int) {
        this.page = page
        LFMRequester(getApplication(), viewModelScope, friendsReceiver).getFriends(page, username)
    }

    fun loadFriendsRecents(user: String) {
        LFMRequester(getApplication(), viewModelScope, tracksReceiver).getFriendsRecents(user)
    }

    fun putFriends(users: Collection<User>, replace: Boolean) {
        if (replace)
            friendsOrig.clear()
        friendsOrig.addAll(users.map { it.toUserSerializable() })
        filterFriends()
    }

    fun addPin(user: UserSerializable, save: Boolean = true): Boolean {
        if (!prefs.proStatus || pinnedFriends.size >= Stuff.MAX_PINNED_FRIENDS) return false

        user.order = pinnedFriends.size
        return if (pinnedUsernames.add(user.name)) {
            pinnedFriends.add(user)
            if (save) {
                savePinnedFriends()
                filterFriends()
            }
            true
        } else {
            false
        }
    }

    fun removePin(user: UserSerializable): Boolean {
        pinnedUsernames -= user.name
        val removed = pinnedFriends.remove(user)
        if (removed) {
            savePinnedFriends()
            filterFriends()
        }
        return removed
    }

    fun isPinned(username: String) = username in pinnedUsernames

    fun savePinnedFriends() {
        prefs.pinnedFriendsJson = pinnedFriends
    }

    private fun filterFriends() {
        friendsFiltered.clear()
        friendsFiltered.addAll(friendsOrig.filter { !isPinned(it.name) })
    }

    fun refreshPins() {
        val lastfmSession by lazy {
            Session.createSession(
                Stuff.LAST_KEY,
                Stuff.LAST_SECRET,
                prefs.lastfmSessKey
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            supervisorScope {
                var modifiedCount = 0
                val now = System.currentTimeMillis()
                pinnedFriends
                    .filter { now - it.lastUpdated > Stuff.PINNED_FRIENDS_CACHE_TIME }
                    .mapConcurrently(2) { userSerializable ->
                        kotlin.runCatching {
                            User.getInfo(userSerializable.name, lastfmSession)
                        }.onSuccess { user ->
                            userSerializable.updateFromUser(user)
                            modifiedCount++
                        }
                    }

                if (modifiedCount > 0) {
                    savePinnedFriends()
                }
            }
        }
    }
}