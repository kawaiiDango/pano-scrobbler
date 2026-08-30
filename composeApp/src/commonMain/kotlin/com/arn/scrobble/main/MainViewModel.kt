package com.arn.scrobble.main

import androidx.annotation.Keep
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.billing.PurchaseMethod
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.edits.EditScrobbleUtils
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.VariantStuff
import com.arn.scrobble.utils.redactedMessage
import com.arn.scrobble.work.CommonWorkState
import com.arn.scrobble.work.DigestWork
import com.arn.scrobble.work.DigestWorker
import com.arn.scrobble.work.PendingScrobblesWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlin.time.Duration.Companion.seconds

class MainViewModel : ViewModel() {

    val drawerDataMap = mutableStateMapOf<UserCached, DrawerData>()

    private val repository = VariantStuff.billingRepository

    val formattedPrice = repository.formattedPrice

    val editScrobbleUtils = EditScrobbleUtils(viewModelScope)

    private val _scrobblerStateFlow = MutableStateFlow<ScrobblerState>(ScrobblerState.Unknown)
    val scrobblerStateFlow = _scrobblerStateFlow.asStateFlow()

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

        updateScrobblerServiceState(true)
    }

    fun updateScrobblerServiceState(requestRebind: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (requestRebind)
                delay(2.seconds)

            val state = PlatformStuff.checkScrobblerState(requestRebind)
            _scrobblerStateFlow.value = state

            if (!killedReasonReported && requestRebind &&
                state is ScrobblerState.Killed && state.reason?.isProbablySystemKill == true
            ) {
                killedReasonReported = true
                val message = state.reason.formatted()
                val pss = " ${state.reason.pssMb} MB"
                val importance = " ${state.reason.importance} i"
                val notiState = if (!state.reason.fgNoti) " noFgNoti" else ""
                Logger.e(AppExitException(message + pss + importance + notiState)) { message }
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
            delay(2.seconds)
            repository.queryPurchasesAsync()
        }
    }

    fun makePurchase(purchaseMethod: PurchaseMethod, activity: Any?) {
        repository.launchBillingFlow(purchaseMethod, activity)
    }

    override fun onCleared() {
        repository.endDataSourceConnections()
    }

    suspend fun loadDrawerData(user: UserCached) {
        val exists = user in drawerDataMap

        if (exists)
            delay(2.seconds)

        if (user.isSelf) {
            PlatformStuff.mainPrefs.data.map {
                it.drawerData[it.currentAccountType]
            }.first()
                ?.let { drawerDataMap[user] = it }
        }

        val scrobblable = Scrobblables.current
        scrobblable
            ?.loadDrawerData(user.name)
            ?.onSuccess { dd ->
                if (user.isSelf) {
                    PlatformStuff.mainPrefs.updateData { p ->
                        p.copy(
                            drawerData = p.drawerData + (p.currentAccountType to dd),
                            scrobbleAccounts = p.scrobbleAccounts.map {
                                if (it.type == p.currentAccountType &&
                                    dd.profilePicUrl != null &&
                                    dd.profilePicUrl != it.user.largeImage
                                ) {
                                    val u = it.user.copy(largeImage = dd.profilePicUrl)
                                    it.copy(user = u)
                                } else {
                                    it
                                }
                            }
                        )
                    }
                }
                drawerDataMap[user] = dd
            }
    }

    companion object {
        private var killedReasonReported = false
    }
}

@Keep
private class AppExitException(override val message: String) : RuntimeException()
