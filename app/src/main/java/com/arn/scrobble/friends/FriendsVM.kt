package com.arn.scrobble.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.friends.UserCached.Companion.toUserCached
import com.arn.scrobble.recents.PaletteColors
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.doOnSuccessLoggingFaliure
import com.arn.scrobble.utils.Stuff.mapConcurrently
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.max


class FriendsVM : ViewModel() {
    private val mainPrefs = PlatformStuff.mainPrefs
    private val lastPlayedTracksMap = mutableMapOf<String, Result<Track>>()
    private val playCountsMap = mutableMapOf<String, Int>()
    val urlToPaletteMap = mutableMapOf<String, PaletteColors>()
    private val _input = MutableStateFlow<MusicEntryLoaderInput?>(null)
    val input = _input.asStateFlow()
    var showsPins = false
        private set
    private val _pinnedFriends = combine(input.filterNotNull(),
        mainPrefs.data.map { it.pinnedFriends.sortedBy { it.order } }) { input, pinnedFriends ->
        showsPins = input.user.isSelf &&
                Scrobblables.current.value?.userAccount?.type == AccountType.LASTFM &&
                Stuff.billingRepository.isLicenseValid

        if (showsPins)
            pinnedFriends
        else
            emptyList()
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val _pinnedUsernamesSet = _pinnedFriends
        .map { it.map { it.name }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    private val _friends = MutableStateFlow<List<UserCached>?>(null) // from network
    private val _tracksReceiver = MutableStateFlow<Pair<String, Result<PageResult<Track>>>?>(null)
    val friendsCombined = combine(
        _friends.filterNotNull(),
        _pinnedFriends,
        _tracksReceiver
    ) { friends, pinnedFriends, tracksReceiver ->

        if (tracksReceiver != null) {
            val (usernameForTrack, trackPageResult) = tracksReceiver

            lastPlayedTracksMap[usernameForTrack] = trackPageResult.map { it.entries.first() }
            playCountsMap[usernameForTrack] = trackPageResult.getOrNull()?.attr?.total ?: 0

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

    private val friendsRecentsSemaphore = Semaphore(2)

    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()

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
                    loadFriends(input.page, input.user.name)
                }
        }
        viewModelScope.launch {
            refreshPins()
        }
    }


    fun setInput(input: MusicEntryLoaderInput, initial: Boolean = false) {
        if (initial && _input.value == null || !initial)
            _input.value = input
    }

    suspend fun loadFriendsRecents(username: String) {
        if (lastPlayedTracksMap[username]?.isFailure == true) return

        friendsRecentsSemaphore.withPermit {
            Scrobblables.current.value!!.getRecents(
                1,
                username,
                limit = 1,
                includeNowPlaying = true,
                cached = !Stuff.isOnline
            )
        }.mapCatching { pr ->
            if (pr.entries.isEmpty())
                throw ApiException(-1, PlatformStuff.application.getString(R.string.no_scrobbles))

            pr
        }
            .let { result ->
                _tracksReceiver.emit(username to result)
            }
    }


    private suspend fun loadFriends(
        page: Int,
        username: String,
        setLoading: Boolean = true
    ) {
        if (setLoading)
            _hasLoaded.emit(false)

        val pr = Scrobblables.current.value!!.getFriends(
            page, username, cached = !loadedInitialCachedVersion
        )

        if (!loadedInitialCachedVersion) {
            loadedInitialCachedVersion = true
            viewModelScope.launch {
                loadFriends(page, username, setLoading = false)
            }
        }

        pr.onSuccess {
            totalPages = max(1, it.attr.totalPages) //dont let totalpages be 0
            _total.emit(it.attr.total!!)
            sorted = false
            lastFriendsLoadTime = System.currentTimeMillis()
        }.onFailure {
            if ((it as? ApiException)?.code == 504)
                return
        }

        emitUsers(pr, page > 1)

//        if (setLoading)
        _hasLoaded.emit(true)
    }


    private suspend fun emitUsers(result: Result<PageResult<User>>, concat: Boolean) {
        result.map {
            if (concat)
                (_friends.value ?: emptyList()) + it.entries.map { it.toUserCached() }
            else
                it.entries.map { it.toUserCached() }
        }.doOnSuccessLoggingFaliure {
            _friends.emit(it)
        }
    }

    suspend fun addPinAndSave(user: UserCached): Boolean {
        if (!Stuff.billingRepository.isLicenseValid || _pinnedFriends.value.size >= Stuff.MAX_PINNED_FRIENDS) return false

        val newUser = user.copy(order = _pinnedFriends.value.size)

        val willBeAdded = newUser.name !in _pinnedUsernamesSet.value

        if (willBeAdded) {
            mainPrefs.updateData { it.copy(pinnedFriends = it.pinnedFriends + newUser) }
        }
        return willBeAdded
    }

    suspend fun removePinAndSave(user: UserCached): Boolean {
        val willBeRemoved = user.name in _pinnedUsernamesSet.value

        if (willBeRemoved) {
            mainPrefs.updateData { it.copy(pinnedFriends = it.pinnedFriends - user) }
        }
        return willBeRemoved
    }

    fun isPinned(username: String) = username in _pinnedUsernamesSet.value

    fun savePinnedFriends(newList: List<UserCached>) {
        viewModelScope.launch {
            mainPrefs.updateData { it.copy(pinnedFriends = newList) }
        }
    }

    fun sortByTime() {
        viewModelScope.launch {
            val newList = _friends.value!!.sortedByDescending {
                if (lastPlayedTracksMap[it.name]?.isSuccess == true) //put users with no tracks at the end
                    lastPlayedTracksMap[it.name]!!.getOrNull()?.date
                        ?: System.currentTimeMillis()
                else
                    0
            }
            sorted = true

            _friends.emit(newList)
        }
    }

    private suspend fun refreshPins() {
        supervisorScope {
            val lastfmSession =
                Scrobblables.all.value
                    .firstOrNull { it.userAccount.type == AccountType.LASTFM } as? LastFm
                    ?: return@supervisorScope
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
                savePinnedFriends(newPinnedFriends)
            }
        }
    }

    data class FriendsItemHolder(
        val user: UserCached,
        val trackResult: Result<Track>?,
        val playCount: Int,
        val isPinned: Boolean
    )
}