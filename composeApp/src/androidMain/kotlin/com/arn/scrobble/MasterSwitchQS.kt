package com.arn.scrobble

import android.content.ComponentName
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import co.touchlab.kermit.Logger
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MasterSwitchQS : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onTileAdded() {
        scope.launch {
            requestListeningState()
        }
    }

    override fun onClick() {
        scope.launch {
            var newState: Boolean? = null
            PlatformStuff.mainPrefs.updateData {
                newState = !it.scrobblerEnabled
                it.copy(scrobblerEnabled = !it.scrobblerEnabled)
            }

            newState?.let { setActive(it) }
        }
    }

    override fun onStartListening() {
        AndroidStuff.applicationContext = applicationContext

        scope.launch {
            val isActive = PlatformStuff.mainPrefs.data.map { it.scrobblerEnabled }.first()
            setActive(isActive)
        }
    }

    private fun setActive(isActive: Boolean) {
        qsTile?.let { qsTile ->
            if (isActive) {
                qsTile.state = Tile.STATE_ACTIVE
                qsTile.label = getString(R.string.scrobbler_on)
            } else {
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.label = getString(R.string.scrobbler_off)
            }
            qsTile.updateTile()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        suspend fun requestListeningState() {
            try {
                withContext(Dispatchers.IO) {
                    requestListeningState(
                        AndroidStuff.applicationContext,
                        ComponentName(
                            AndroidStuff.applicationContext,
                            MasterSwitchQS::class.java
                        )
                    )
                }
            } catch (e: Exception) {
                Logger.w(e) { "Failed to update QS tile state" }
            }
        }
    }
}