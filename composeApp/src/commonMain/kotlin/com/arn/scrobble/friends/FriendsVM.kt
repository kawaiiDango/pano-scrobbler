package com.arn.scrobble.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.UserCached.Companion.toUserCached
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.no_scrobbles
import pano_scrobbler.composeapp.generated.resources.pin_limit_reached


class FriendsVM : ViewModel() {
    private val mainPrefs = PlatformStuff.mainPrefs
    private val _friendsExtraDataMap =
        MutableStateFlow<Map<String, FriendExtraData>>(emptyMap())
    val friendsExtraDataMap = _friendsExtraDataMap.asStateFlow()
    private val _totalCount = MutableStateFlow(0)
    val totalFriends = _totalCount.asStateFlow()

    private val user = MutableStateFlow<UserCached?>(null)

    val pinnedFriends = mainPrefs.data.map {
        it.pinnedFriends[it.currentAccountType]
            ?.sortedBy { it.order }
            ?: emptyList()
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pinnedUsernamesSet = pinnedFriends.mapLatest { it.map { it.name }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    private val friendsRecentsMutex = Mutex()

    private val _lastFriendsRefreshTime = MutableStateFlow(System.currentTimeMillis())
    val lastFriendsRefreshTime = _lastFriendsRefreshTime.asStateFlow()

    val friends = user.filterNotNull().flatMapLatest { user ->
        Pager(
            config = PagingConfig(
                pageSize = Stuff.DEFAULT_PAGE_SIZE,
                initialLoadSize = Stuff.DEFAULT_PAGE_SIZE,
                prefetchDistance = 4,
                enablePlaceholders = true
            ),
            pagingSourceFactory = {
                FriendsPagingSource(
                    user.name,
                    onSetLastFriendsRefreshTime = { _lastFriendsRefreshTime.value = it },
                    setTotal = { _totalCount.value = it }
                )
            }
        ).flow
    }
        .cachedIn(viewModelScope)

    private var loadedInitialCachedVersion = false
    private val _sortedFriends = MutableStateFlow<List<UserCached>?>(null)
    val sortedFriends = _sortedFriends.asStateFlow()

    init {
        viewModelScope.launch {
            refreshPins()
        }
    }

    fun setUser(userp: UserCached) {
        user.value = userp
    }

    suspend fun loadFriendsRecents(username: String) {
        if (friendsExtraDataMap.value[username]?.errorMessage != null) return

        delay(Stuff.FRIENDS_RECENTS_DELAY)

        _friendsExtraDataMap.value += username to friendsRecentsMutex.withLock {
            Scrobblables.current.value!!.getRecents(
                1,
                username,
                limit = 1,
                includeNowPlaying = true,
                cached = !Stuff.isOnline
            )
        }.mapCatching { pr ->
            if (pr.entries.isEmpty())
                throw ApiException(-1, getString(Res.string.no_scrobbles))

            FriendExtraData(
                track = pr.entries.first(),
                playCount = pr.attr.total,
                lastUpdated = System.currentTimeMillis()
            )
        }.getOrElse {
            FriendExtraData(
                track = null,
                playCount = null,
                lastUpdated = System.currentTimeMillis(),
                errorMessage = it.redactedMessage
            )
        }
    }

    fun addPinAndSave(user: UserCached): Boolean {
        if (!PlatformStuff.billingRepository.isLicenseValid || pinnedFriends.value.size >= Stuff.MAX_PINNED_FRIENDS) return false

        val newUser = user.copy(order = pinnedFriends.value.size)

        val willBeAdded = newUser.name !in pinnedUsernamesSet.value

        if (willBeAdded) {
            viewModelScope.launch {
                mainPrefs.updateData {
                    it.copy(
                        pinnedFriends = it.pinnedFriends +
                                (it.currentAccountType to (it.pinnedFriends.getOrDefault(
                                    it.currentAccountType, emptySet()
                                ) + newUser))
                    )
                }
            }
        } else {
            viewModelScope.launch {
                val snackbarData = PanoSnackbarVisuals(
                    getString(
                        Res.string.pin_limit_reached,
                        Stuff.MAX_PINNED_FRIENDS
                    ),
                    isError = true
                )
                Stuff.globalSnackbarFlow.emit(snackbarData)
            }
        }
        return willBeAdded
    }

    fun removePinAndSave(user: UserCached): Boolean {
        val willBeRemoved = user.name in pinnedUsernamesSet.value

        if (willBeRemoved) {
            viewModelScope.launch {
                mainPrefs.updateData {
                    it.copy(
                        pinnedFriends = it.pinnedFriends +
                                (it.currentAccountType to (it.pinnedFriends.getOrDefault(
                                    it.currentAccountType, emptySet()
                                ) - user))
                    )
                }
            }
        }

        return willBeRemoved
    }


    fun movePinAndSave(username: String, right: Boolean): Boolean {
        val from = pinnedFriends.value.indexOfFirst { it.name == username }
        if (from == -1) return false
        val to = (if (right) from + 1 else from - 1).coerceIn(0, pinnedFriends.value.size - 1)
        if (from == to) return false

        viewModelScope.launch {
            val newList = pinnedFriends.value.toMutableList()
            val friend = newList.removeAt(from)
            newList.add(to, friend)
            val newListOrdered = newList.mapIndexed { index, it ->
                it.copy(order = index)
            }
            mainPrefs.updateData {
                it.copy(pinnedFriends = it.pinnedFriends + (it.currentAccountType to newListOrdered))
            }
        }

        return true
    }

    fun sortByTime(friends: List<UserCached>) {
        val now = System.currentTimeMillis()
        _sortedFriends.value = friends.sortedByDescending {
            if (friendsExtraDataMap.value[it.name]?.errorMessage != null)
                0 //put users with errors at the end
            else
                friendsExtraDataMap.value[it.name]?.track?.date ?: now
        }
    }

    fun clearSortedFriends() {
        _sortedFriends.value = null
    }

    fun markExtraDataAsStale() {
        _friendsExtraDataMap.value = _friendsExtraDataMap.value.mapValues { (key, value) ->
            value.copy(lastUpdated = 0)
        }
    }

    private suspend fun refreshPins() {
        supervisorScope {
            val lastfmSession =
                Scrobblables.current.value as? LastFm
                    ?: return@supervisorScope
            var modifiedCount = 0
            val now = System.currentTimeMillis()
            val newPinnedFriends = pinnedFriends.value
                .filter { now - it.lastUpdated > Stuff.PINNED_FRIENDS_CACHE_TIME }
                .mapConcurrently(2) { userSerializable ->

                    lastfmSession.userGetInfo(userSerializable.name)
                        .map { user ->
                            modifiedCount++
                            user.toUserCached()
                        }.getOrDefault(userSerializable)
                }

            if (modifiedCount > 0) {
                mainPrefs.updateData {
                    it.copy(
                        pinnedFriends = it.pinnedFriends + (it.currentAccountType to newPinnedFriends)
                    )
                }
            }
        }
    }
}