package com.arn.scrobble

import android.os.Build
import android.preference.PreferenceManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class MasterSwitchQS: TileService() {

    override fun onClick() {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val isActive = qsTile.state == Tile.STATE_ACTIVE
        pref.edit().putBoolean(Stuff.PREF_MASTER, !isActive).apply()
        setActive(!isActive)
    }

    override fun onStartListening() {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        setActive(pref.getBoolean(Stuff.PREF_MASTER, true))
    }

    private fun setActive(isActive: Boolean){
        if (isActive){
            qsTile.state = Tile.STATE_ACTIVE
            qsTile.label = getString(R.string.qs_master_on)
        } else {
            qsTile.state = Tile.STATE_INACTIVE
            qsTile.label = getString(R.string.qs_master_off)
        }
        qsTile.updateTile()
    }
}