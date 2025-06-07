package com.arn.scrobble.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.github.GithubReleases
import com.arn.scrobble.api.github.Github
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.redactedMessage
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.friends.FriendExtraData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainViewModel : ViewModel() {

    private var lastDrawerDataRefreshTime = 0L
    private var lastDrawerDataFetchUser: UserCached? = null
    private var lastDrawerDataOthersCached: DrawerData? = null
    private val mainPrefs = PlatformStuff.mainPrefs

    val canIndex = mainPrefs.data.map { it.lastMaxIndexTime }.map {
        PlatformStuff.isDebug && Scrobblables.current.value is LastFm &&
                System.currentTimeMillis() - (it ?: 0) > TimeUnit.HOURS.toMillis(12)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val _editData = MutableSharedFlow<Pair<Track, ScrobbleData>>()
    val editDataFlow = _editData.asSharedFlow()

    private val _updateAvailablity = MutableSharedFlow<GithubReleases>()
    val updateAvailability = _updateAvailablity.asSharedFlow()

    val friendExtraData = MutableSharedFlow<FriendExtraData>()

    private val _pullToRefreshTriggered = MutableSharedFlow<Int>()

    private val _currentUser = MutableStateFlow<UserCached?>(null)

    private val repository = PlatformStuff.billingRepository

    val proProductDetails = repository.proProductDetails

    private val _selectedPackages = MutableSharedFlow<Pair<List<AppItem>, List<AppItem>>>()
    val selectedPackages = _selectedPackages.asSharedFlow()

    val drawerData = mainPrefs.data.map { it.drawerData }
        .combine(_currentUser) { drawerData, user ->

            if (user?.isSelf == true) {
                Scrobblables.current.value?.userAccount?.type?.let { type ->
                    drawerData[type]
                }
            } else if (user != null) {
                lastDrawerDataRefreshTime = System.currentTimeMillis()

                Scrobblables.current.value
                    ?.loadDrawerData(user.name)

            } else
                null
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    var pendingSubmitAttempted = false

    val isItChristmas by lazy {
        val cal = Calendar.getInstance()
        PlatformStuff.isDebug ||
                (cal.get(Calendar.MONTH) == Calendar.DECEMBER && cal.get(Calendar.DAY_OF_MONTH) >= 24) ||
                (cal.get(Calendar.MONTH) == Calendar.JANUARY && cal.get(Calendar.DAY_OF_MONTH) <= 7)
    }

    // A flow to represent trigger signals for refreshing
    private val otherUserTrigger = MutableSharedFlow<UserCached?>(extraBufferCapacity = 1)

    // The main flow that handles caching, network calls, and emits values
    val drawerDataFlow: StateFlow<DrawerData> =
        combine(
            Scrobblables.current.map { it?.userAccount?.user },
            otherUserTrigger
        )
        { userSelf, userOther -> userSelf to userOther }
            .flatMapLatest { (userSelf, userOther) ->
                val user = userOther ?: userSelf
                if (user == null) {
                    flowOf(DrawerData(0))
                } else {
                    val now = System.currentTimeMillis()
                    val lastCall = lastDrawerDataRefreshTime
                    if (now - lastCall >= 5 * 60 * 1000 || user != lastDrawerDataFetchUser) {
                        flow {
                            // Fetch fresh data
                            val freshData = Scrobblables.current.value
                                ?.loadDrawerData(user.name)
                            lastDrawerDataRefreshTime = System.currentTimeMillis()
                            lastDrawerDataFetchUser = user

                            if (freshData != null) {
                                emit(freshData)
                            }
                        }
                    } else if (user.isSelf) {
                        PlatformStuff.mainPrefs.data.map { it.drawerData }
                            .map {
                                it[Scrobblables.current.value?.userAccount?.type] ?: DrawerData(0)
                            }
                    } else {
                        flowOf(lastDrawerDataOthersCached ?: DrawerData(0))
                    }

                }
            }
//            .onStart {
//                emit(
//                    PlatformStuff.mainPrefs.data.map {
//                        it.drawerData[it.currentAccountType] ?: DrawerData(0)
//                    }
//                        .first()
//                )
//            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = Stuff.mainPrefsInitialValue.let { it.drawerData[it.currentAccountType] }
                    ?: DrawerData(0)
            )

    init {
        Stuff.globalExceptionFlow.mapLatest { e ->
            if (PlatformStuff.isDebug)
                e.printStackTrace()

            if (e is ApiException && e.code != 504) { // suppress cache not found exceptions
                Stuff.globalSnackbarFlow.emit(
                    PanoSnackbarVisuals(
                        message = e.redactedMessage,
                        isError = true
                    )
                )
            }

            if (e is SerializationException) {
                Logger.w(e.cause) { "SerializationException" }
            }
        }.launchIn(viewModelScope)

        repository.initBillingClient()
        repository.startDataSourceConnections()

        queryPurchasesAsync()

        checkForUpdatesIfNeeded()
    }

    fun checkAndStoreLicense(receipt: String) {
        viewModelScope.launch {
            repository.checkAndStoreLicense(receipt)
        }
    }

    fun queryPurchasesAsync() {
        viewModelScope.launch {
            delay(2000)
            repository.queryPurchasesAsync()
        }
    }

    fun makePlayPurchase(activity: Any) {
        repository.launchPlayBillingFlow(activity)
    }

    // pass null to load self user
    fun loadOtherUserDrawerData(user: UserCached?) {
        viewModelScope.launch {
            otherUserTrigger.emit(user)
        }
    }

    override fun onCleared() {
        repository.endDataSourceConnections()
//        PanoDb.destroyInstance()
        super.onCleared()
    }


    fun onSetPackagesSelection(checked: List<AppItem>, unchecked: List<AppItem>) {
        viewModelScope.launch {
            _selectedPackages.emit(checked to unchecked)
        }
    }

    fun notifyEdit(origTrack: Track, sd: ScrobbleData) {
        viewModelScope.launch {
            _editData.emit(origTrack to sd)
        }
    }

    fun notifyPullToRefresh(id: Int) {
        viewModelScope.launch {
            _pullToRefreshTriggered.emit(id)
        }
    }


    fun getPullToRefreshTrigger(id: Int) =
        _pullToRefreshTriggered
            .filter { it == id }
            .map { }

    private fun checkForUpdatesIfNeeded() {
        if (!PlatformStuff.isNonPlayBuild)
            return

        viewModelScope.launch(Dispatchers.IO) {
            delay(3000)
            val checkForUpdates = mainPrefs.data.map { it.checkForUpdates }.first()

            if (!checkForUpdates) {
                return@launch
            }

            val lastUpdateCheckTime = mainPrefs.data.map { it.lastUpdateCheckTime }.first()
            Github.checkForUpdates(
                client = Requesters.genericKtorClient,
                json = Stuff.myJson,
                currentVersionCode = BuildKonfig.VER_CODE,
                lastUpdateCheckTime = lastUpdateCheckTime ?: 0,
                setLastUpdateCheckTime = { time ->
                    mainPrefs.updateData { it.copy(lastUpdateCheckTime = time) }
                }
            )?.let {
                _updateAvailablity.emit(it)
            }
        }
    }
}