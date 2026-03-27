package com.arn.scrobble.main

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.billing.PurchaseMethod
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.edits.EditScrobbleUtils
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.VariantStuff
import com.arn.scrobble.utils.redactedMessage
import com.arn.scrobble.work.CommonWorkState
import com.arn.scrobble.work.DigestWork
import com.arn.scrobble.work.DigestWorker
import com.arn.scrobble.work.PendingScrobblesWork
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.util.Calendar

class MainViewModel : ViewModel() {

    val drawerDataMap = mutableStateMapOf<UserCached, DrawerData>()

    private val _pullToRefreshTriggered = MutableSharedFlow<Int>()

    private val repository = VariantStuff.billingRepository

    val formattedPrice = repository.formattedPrice

    private val _selectedPackages = MutableSharedFlow<Pair<List<AppItem>, List<AppItem>>>()
    val selectedPackages = _selectedPackages.asSharedFlow()

    val isItChristmas by lazy {
        val cal = Calendar.getInstance()
        BuildKonfig.DEBUG ||
                (cal.get(Calendar.MONTH) == Calendar.DECEMBER && cal.get(Calendar.DAY_OF_MONTH) >= 24) ||
                (cal.get(Calendar.MONTH) == Calendar.JANUARY && cal.get(Calendar.DAY_OF_MONTH) <= 5)
    }

    val editScrobbleUtils = EditScrobbleUtils(viewModelScope)

    init {
        Stuff.globalExceptionFlow.map { e ->
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

        // schedule pending scrobbles work on UI start
        viewModelScope.launch {
            val force = PanoDb.db.getPendingScrobblesDao().canForceRetry()
            val hasPending = if (!force)
                PanoDb.db.getPendingScrobblesDao().count() > 0
            else
                true

            val isRunning = PendingScrobblesWork.state().first() == CommonWorkState.RUNNING

            if (hasPending && !isRunning) {
                PendingScrobblesWork.schedule(force)
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

    fun makePurchase(purchaseMethod: PurchaseMethod, activity: Any?) {
        repository.launchBillingFlow(purchaseMethod, activity)
    }

    override fun onCleared() {
        repository.endDataSourceConnections()
    }


    fun onSetPackagesSelection(checked: List<AppItem>, unchecked: List<AppItem>) {
        viewModelScope.launch {
            _selectedPackages.emit(checked to unchecked)
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
}