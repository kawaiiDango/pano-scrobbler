package com.arn.scrobble

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.arn.scrobble.pref.MainPrefs

@RequiresApi(Build.VERSION_CODES.N)
class MasterSwitchQS : TileService() {

    override fun onClick() {
        qsTile ?: return
        val isActive = qsTile.state == Tile.STATE_ACTIVE
        MainPrefs(applicationContext).scrobblerEnabled = !isActive
        setActive(!isActive)
    }

    override fun onStartListening() {
        qsTile ?: return
        val isActive = MainPrefs(applicationContext).scrobblerEnabled
        setActive(isActive)
    }

    private fun setActive(isActive: Boolean) {
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