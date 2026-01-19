package com.arn.scrobble.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.billing.PurchaseMethod
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.VariantStuff
import com.arn.scrobble.utils.redactedMessage
import com.arn.scrobble.work.DigestWork
import com.arn.scrobble.work.DigestWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
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

    val canIndex = mainPrefs.data
        .map { prefs ->
            (prefs.lastMaxIndexTime ?: 0).takeIf { prefs.currentAccountType == AccountType.LASTFM }
        }
        .filterNotNull()
        .map {
            BuildKonfig.DEBUG &&
                    System.currentTimeMillis() - it > TimeUnit.HOURS.toMillis(12)
        }

    private val _editData = MutableSharedFlow<Pair<String, Track>>(extraBufferCapacity = 1)
    val editDataFlow = _editData.asSharedFlow()
    private val _pullToRefreshTriggered = MutableSharedFlow<Int>()

    private val repository = VariantStuff.billingRepository

    val proProductDetails = repository.proProductDetails

    private val _selectedPackages = MutableSharedFlow<Pair<List<AppItem>, List<AppItem>>>()
    val selectedPackages = _selectedPackages.asSharedFlow()

    var pendingSubmitAttempted = false

    val isItChristmas by lazy {
        val cal = Calendar.getInstance()
        BuildKonfig.DEBUG ||
                (cal.get(Calendar.MONTH) == Calendar.DECEMBER && cal.get(Calendar.DAY_OF_MONTH) >= 24) ||
                (cal.get(Calendar.MONTH) == Calendar.JANUARY && cal.get(Calendar.DAY_OF_MONTH) <= 5)
    }

    // A flow to represent trigger signals for refreshing
    private val otherUserTrigger = MutableSharedFlow<UserCached?>(extraBufferCapacity = 1)

    // The main flow that handles caching, network calls, and emits values
    // todo do it in a backstack aware way
    val drawerDataFlow: StateFlow<DrawerData> =
        combine(
            PlatformStuff.mainPrefs.data.map { prefs ->
                prefs.currentAccount?.user
            },
            otherUserTrigger.distinctUntilChanged()
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
                            val freshData = Scrobblables.current
                                ?.loadDrawerData(user.name)
                            lastDrawerDataRefreshTime = System.currentTimeMillis()
                            lastDrawerDataFetchUser = user

                            if (freshData != null) {
                                emit(freshData)
                            }
                        }
                    } else if (user.isSelf) {
                        PlatformStuff.mainPrefs.data.map {
                            it.drawerData[it.currentAccountType]
                                ?: DrawerData(0)
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
                initialValue = DrawerData(0)
            )

    init {
        Stuff.globalExceptionFlow.mapLatest { e ->
            if (BuildKonfig.DEBUG)
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

        if (!PlatformStuff.isDesktop && !PlatformStuff.isTv)
            viewModelScope.launch {
                if (DigestWork.state().first() == null) {
                    val (nextWeek, nextMonth) = DigestWorker.nextWeekAndMonth()

                    DigestWork.schedule(
                        nextWeek,
                        nextMonth,
                    )
                }
            }
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

    fun makePurchase(purchaseMethod: PurchaseMethod, activity: Any) {
        repository.launchBillingFlow(purchaseMethod, activity)
    }

    // pass null to load self user
    fun loadOtherUserDrawerData(user: UserCached?) {
        viewModelScope.launch {
            otherUserTrigger.emit(user)
        }
    }

    override fun onCleared() {
        repository.endDataSourceConnections()
        super.onCleared()
    }


    fun onSetPackagesSelection(checked: List<AppItem>, unchecked: List<AppItem>) {
        viewModelScope.launch {
            _selectedPackages.emit(checked to unchecked)
        }
    }

    fun notifyEdit(key: String, track: Track) {
        _editData.tryEmit(key to track)
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
}