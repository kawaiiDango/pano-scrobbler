package com.arn.scrobble.friends

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.App
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.mapConcurrently
import com.arn.scrobble.friends.UserSerializable.Companion.toUserSerializable
import com.arn.scrobble.recents.PaletteColors
import com.arn.scrobble.scrobbleable.AccountType
import com.arn.scrobble.scrobbleable.Scrobblables
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
import kotlinx.coroutines.withContext


class FriendsVM(app: Application) : AndroidViewModel(app) {
    val friendsFiltered = mutableListOf<UserSerializable>()
    private val friendsOrig = mutableListOf<UserSerializable>()
    private val prefs = App.prefs
    val pinnedFriends = mutableListOf<UserSerializable>()
    private val pinnedUsernames = hashSetOf<String>()
    val lastPlayedTracksMap = mutableMapOf<String, Track?>()
    val playCountsMap = mutableMapOf<String, Int>()
    val urlToPaletteMap = mutableMapOf<String, PaletteColors>()
    val friendsReceiver = LiveEvent<PaginatedResult<User>?>()
    val tracksReceiver = LiveEvent<Pair<String, PaginatedResult<Track>>>()
    val privateUsers = mutableSetOf<String>() // todo this is a hack
    private val errorNotifier = LFMRequester.ExceptionNotifier()
    var showsPins = false
    var page = 1
    var totalPages = 1
    var total = 0
    var sorted = false
    var hasLoaded = false

    val sectionedList by lazy {

        if (showsPins)
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

    fun loadFriendsList(page: Int, user: UserSerializable) {
        this.page = page
        viewModelScope.launch(errorNotifier) {
            friendsReceiver.value = withContext(Dispatchers.IO) {
                Scrobblables.current!!.getFriends(page, user.name)
            }
        }
    }

    fun loadFriendsRecents(username: String) {
        viewModelScope.launch(errorNotifier) {
            val pr = withContext(Dispatchers.IO) {
                Scrobblables.current!!.getRecents(
                    1,
                    username,
                    limit = 1,
                )
            }

            if (pr.pageResults != null && pr.pageResults.isNotEmpty())
                tracksReceiver.value = username to pr
            else
                privateUsers += username
        }
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
                Scrobblables.byType(AccountType.LASTFM)!!.userAccount.authKey
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