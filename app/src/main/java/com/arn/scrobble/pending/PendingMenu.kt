package com.arn.scrobble.pending

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.db.PendingScrobblesDb
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.RuntimeException

object PendingMenu {

    @SuppressLint("RestrictedApi")
    fun openPendingPopupMenu (anchor: View, p: Any, deleteAction: ()-> Unit, loveAction: (()-> Unit)? = null) {
        val context = anchor.context
        val menuBuilder = MenuBuilder(context)
        val inflater = SupportMenuInflater(context)
        inflater.inflate(R.menu.pending_item_menu, menuBuilder)
        if (p is PendingLove)
            menuBuilder.removeItem(R.id.menu_love)

        menuBuilder.setCallback(object : MenuBuilder.Callback {
            override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.menu_details -> {
                        val servicesList = mutableListOf<String>()
                        val state: Int

                        @StringRes
                        val actionRes: Int
                        if (p is PendingScrobble) {
                            state = p.state
                            actionRes = R.string.scrobble
                        } else if (p is PendingLove) {
                            state = p.state
                            actionRes = if (p.shouldLove)
                                R.string.love
                            else
                                R.string.unlove
                        } else
                            throw RuntimeException("Not a Pending Item")
                        Stuff.SERVICE_BIT_POS.forEach { (id, pos) ->
                            if (state and (1 shl pos) != 0)
                                servicesList += context.getString(id)
                        }
                        MaterialAlertDialogBuilder(context)
                                .setMessage(context.getString(R.string.details_text,
                                        context.getString(actionRes).lowercase(),
                                        servicesList.joinToString(", ")))
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                    }
                    R.id.menu_delete -> {
                        AsyncTask.THREAD_POOL_EXECUTOR.execute {
                            try {
                                if (p is PendingScrobble)
                                    PendingScrobblesDb.getDb(context).getScrobblesDao().delete(p)
                                else if (p is PendingLove)
                                    PendingScrobblesDb.getDb(context).getLovesDao().delete(p)
                                deleteAction.invoke()
                            } catch (e: Exception) {
                            }
                        }
                    }
                    R.id.menu_love -> {
                        if (p is PendingScrobble) {
                            LFMRequester(context!!).loveOrUnlove(true, p.artist, p.track) {
                                        if (!it) {
                                            if (loveAction != null)
                                                loveAction.invoke()
                                            else
                                                deleteAction.invoke()
                                        }
                                    }
                                    .asAsyncTask()
                        }
                    }
                }
                return true
            }

            override fun onMenuModeChange(menu: MenuBuilder) {}
        })

        val popupMenu = MenuPopupHelper(context!!, menuBuilder, anchor)
        popupMenu.setForceShowIcon(true)
        popupMenu.show()
    }
}