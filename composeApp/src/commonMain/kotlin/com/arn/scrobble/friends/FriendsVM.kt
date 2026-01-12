package com.arn.scrobble.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.filter
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.UserCached.Companion.toUserCached
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import com.arn.scrobble.utils.VariantStuff
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.no_scrobbles
import pano_scrobbler.composeapp.generated.resources.pin_limit_reached


class FriendsVM(user: UserCached) : ViewModel() {
    private val mainPrefs = PlatformStuff.mainPrefs
    private val _friendsExtraDataMap = MutableStateFlow<Map<String, FriendExtraData>>(emptyMap())
    val friendsExtraDataMap = _friendsExtraDataMap.asStateFlow()
    private val _totalCount = MutableStateFlow(0)
    val totalFriends = _totalCount.asStateFlow()

    val pinnedFriends = mainPrefs.data.map {
        it.pinnedFriends[it.currentAccountType]
            ?.distinctBy { it.name } // hotfix for crash
            ?.sortedBy { it.order }
            ?: emptyList()
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val friendsRecentsMutex = Mutex()

    private val _lastFriendsRefreshTime = MutableStateFlow(System.currentTimeMillis())
    val lastFriendsRefreshTime = _lastFriendsRefreshTime.asStateFlow()

    val friends = Pager(
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
        .cachedIn(viewModelScope)
        .map { pagingData ->
            val keysTillNow = mutableSetOf<String>()

            pagingData.filter {
                val key = it.name
                val keep = key !in keysTillNow
                keysTillNow += key
                keep
            }
        }

    private var loadedInitialCachedVersion = false
    private val _sortedFriends = MutableStateFlow<List<UserCached>?>(null)
    val sortedFriends = _sortedFriends.asStateFlow()

    init {
        viewModelScope.launch {
            refreshPins()
        }
    }

    suspend fun loadFriendsRecents(username: String) {
        if (friendsExtraDataMap.value[username]?.errorMessage != null) return

        delay(Stuff.FRIENDS_RECENTS_DELAY)

        _friendsExtraDataMap.value += username to friendsRecentsMutex.withLock {
            Scrobblables.current!!.getRecents(
                1,
                username,
                limit = 1,
                includeNowPlaying = true,
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

    fun addPinAndSave(user: UserCached) {
        if (!VariantStuff.billingRepository.isLicenseValid) return

        val newUser = user.copy(order = pinnedFriends.value.size)

        if (pinnedFriends.value.size < Stuff.MAX_PINNED_FRIENDS) {
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
    }

    fun removePinAndSave(user: UserCached) {
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


    fun savePins(pinnedFriends: List<UserCached>) {
        viewModelScope.launch {
            mainPrefs.updateData {
                it.copy(pinnedFriends = it.pinnedFriends + (it.currentAccountType to pinnedFriends))
            }
        }
    }

    fun movePin(pinnedFriends: List<UserCached>, from: Int, to: Int): List<UserCached> {
        if (from < 0 || from >= pinnedFriends.size || to < 0 || to >= pinnedFriends.size) return pinnedFriends
        if (from == to) return pinnedFriends

        val newList = pinnedFriends.toMutableList()
        val friend = newList.removeAt(from)
        newList.add(to, friend)

        return newList.mapIndexed { index, it ->
            it.copy(order = index)
        }
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
                Scrobblables.current as? LastFm
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