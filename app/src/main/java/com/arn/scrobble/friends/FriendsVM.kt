package com.arn.scrobble.friends

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.App
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters.toFlow
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.friends.UserCached.Companion.toUserCached
import com.arn.scrobble.recents.PaletteColors
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.max


class FriendsVM(app: Application) : AndroidViewModel(app) {
    private val prefs = App.prefs
    private val lastPlayedTracksMap = mutableMapOf<String, Track?>()
    private val playCountsMap = mutableMapOf<String, Int>()
    val urlToPaletteMap = mutableMapOf<String, PaletteColors>()
    private val _pinnedFriends = MutableStateFlow<List<UserCached>>(emptyList()) // from json
    private val _pinnedUsernamesSet = _pinnedFriends
        .map { it.map { it.name }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val pinnedFriends = _pinnedFriends.asStateFlow()
    private val _friends = MutableStateFlow<List<UserCached>?>(null) // from network
    private val _tracksReceiver = MutableStateFlow<Pair<String, PageResult<Track>>?>(null)
    val friendsCombined = combine(
        _friends.filterNotNull(),
        _pinnedFriends,
        _tracksReceiver
    ) { friends, pinnedFriends, tracksReceiver ->

        if (tracksReceiver != null) {
            val (usernameForTrack, trackPage) = tracksReceiver
            lastPlayedTracksMap[usernameForTrack] = trackPage.entries.firstOrNull()
            playCountsMap[usernameForTrack] = trackPage.attr.total ?: 0

            delay(100)
        }

        (pinnedFriends + friends.filter { it.name !in _pinnedUsernamesSet.value }).map { user ->
            FriendsItemHolder(
                user,
                lastPlayedTracksMap[user.name],
                playCountsMap[user.name] ?: 0,
                user.name in _pinnedUsernamesSet.value
            )
        }
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val privateUsers = mutableSetOf<String>() // todo this is a hack
    private val friendsRecentsSemaphore = Semaphore(2)
    private val _input = MutableStateFlow<MusicEntryLoaderInput?>(null)
    val input = _input.asStateFlow()
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()
    var showsPins = false
        private set
    var totalPages = 1
        private set
    private val _total = MutableStateFlow(0)
    val total = _total.asStateFlow()
    var sorted = false
        private set
    var lastFriendsLoadTime = System.currentTimeMillis()
        private set
    private var loadedInitialCachedVersion = false

    init {
        viewModelScope.launch {
            _input.filterNotNull()
                .collectLatest { input ->
                    showsPins = input.user.isSelf &&
                            Scrobblables.current?.userAccount?.type == AccountType.LASTFM &&
                            prefs.proStatus

                    if (showsPins && _pinnedFriends.value.isEmpty()) {
                        val pinnedJson = prefs.pinnedFriendsJson
                        _pinnedFriends.emit(pinnedJson.sortedBy { it.order })
                    }
                    loadFriends(input.page, input.user.name)

                }
        }
        viewModelScope.launch {
            refreshPins()
        }

        viewModelScope.launch {
            _pinnedFriends.collectLatest { pinnedFriends ->
                if (showsPins)
                    prefs.pinnedFriendsJson = pinnedFriends
            }
        }
    }


    fun setInput(input: MusicEntryLoaderInput, initial: Boolean = false) {
        if (initial && _input.value == null || !initial)
            _input.value = input
    }

    suspend fun loadFriendsRecents(username: String) {
        if (privateUsers.contains(username)) return

        friendsRecentsSemaphore.withPermit {
            Scrobblables.current!!.getRecents(
                1,
                username,
                limit = 1,
                includeNowPlaying = true,
            )
        }.onSuccess { pr ->
            if (pr.entries.isNotEmpty())
                _tracksReceiver.emit(username to pr)
        }.onFailure {
            privateUsers += username
        }
    }


    private suspend fun loadFriends(
        page: Int,
        username: String,
        setLoading: Boolean = true
    ) {
        if (setLoading)
            _hasLoaded.emit(false)

        val pr = Scrobblables.current!!.getFriends(
            page, username, cached = !loadedInitialCachedVersion
        )
            .onSuccess {
                totalPages = max(1, it.attr.totalPages) //dont let totalpages be 0
                _total.emit(it.attr.total!!)
                sorted = false
                lastFriendsLoadTime = System.currentTimeMillis()
            }

        emitUsers(pr, page > 1)

        if (!loadedInitialCachedVersion) {
            loadedInitialCachedVersion = true
            if (pr.getOrNull()?.fromCache == true)
                viewModelScope.launch {
                    loadFriends(page, username, setLoading = false)
                }
        }

        if (setLoading)
            _hasLoaded.emit(true)
    }


    private suspend fun emitUsers(result: Result<PageResult<User>>, concat: Boolean) {
        if (concat)
            _friends.emitAll(result.map {
                (_friends.value ?: emptyList()) + it.entries.map { it.toUserCached() }
            }
                .toFlow())
        else
            _friends.emitAll(result.map { it.entries.map { it.toUserCached() } }.toFlow())
    }

    suspend fun addPinAndSave(user: UserCached): Boolean {
        if (!prefs.proStatus || _pinnedFriends.value.size >= Stuff.MAX_PINNED_FRIENDS) return false

        val newUser = user.copy(order = _pinnedFriends.value.size)

        val willBeAdded = newUser.name !in _pinnedUsernamesSet.value

        if (willBeAdded) {
            _pinnedFriends.emit(_pinnedFriends.value + newUser)
        }
        return willBeAdded
    }

    suspend fun removePinAndSave(user: UserCached): Boolean {
        val willBeRemoved = user.name in _pinnedUsernamesSet.value

        if (willBeRemoved) {
            _pinnedFriends.emit(_pinnedFriends.value - user)
        }
        return willBeRemoved
    }

    fun isPinned(username: String) = username in _pinnedUsernamesSet.value

    fun savePinnedFriends(newList: List<UserCached>) {
        viewModelScope.launch {
            _pinnedFriends.emit(newList)
        }
    }

    fun sortByTime() {
        viewModelScope.launch {
            val newList = _friends.value!!.sortedByDescending {
                if (lastPlayedTracksMap[it.name] == null) //put users with no tracks at the end
                    0
                else
                    lastPlayedTracksMap[it.name]!!.date
                        ?: System.currentTimeMillis()
            }
            sorted = true

            _friends.emit(newList)
        }
    }

    private suspend fun refreshPins() {
        supervisorScope {
            val lastfmSession =
                Scrobblables.byType(AccountType.LASTFM) as? LastFm ?: return@supervisorScope
            var modifiedCount = 0
            val now = System.currentTimeMillis()
            val newPinnedFriends = _pinnedFriends.value
                .filter { now - it.lastUpdated > Stuff.PINNED_FRIENDS_CACHE_TIME }
                .mapConcurrently(2) { userSerializable ->

                    lastfmSession.userGetInfo(userSerializable.name)
                        .map { user ->
                            modifiedCount++
                            user.toUserCached()
                        }.getOrDefault(userSerializable)
                }

            if (modifiedCount > 0) {
                _pinnedFriends.emit(newPinnedFriends)
                savePinnedFriends(newPinnedFriends)
            }
        }
    }

    data class FriendsItemHolder(
        val user: UserCached,
        val track: Track?,
        val playCount: Int,
        val isPinned: Boolean
    )
}