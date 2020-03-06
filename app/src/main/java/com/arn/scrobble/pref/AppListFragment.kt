package com.arn.scrobble.pref

import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.transition.Fade
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.Main
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import kotlinx.android.synthetic.main.content_app_list.*
import java.util.*


/**
 * Created by arn on 05/09/2017.
 */
class AppListFragment : Fragment() {
    private var firstRun = false
    private var appListLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = Fade()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.content_app_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!app_list.isInTouchMode)
            app_list.requestFocus()

        if (Main.isTV){
            app_list_done.visibility = View.GONE
            Stuff.toast(context, getString(R.string.press_back))
        }

        app_list.layoutManager = LinearLayoutManager(context)
        val adapter = AppListAdapter(activity!!)
        app_list.adapter = adapter
        if (!Main.isTV) {
            app_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrollStateChanged(view: RecyclerView, scrollState: Int) {
                    if (scrollState == 0) { //scrolling stopped
                        app_list_done.show()
                    } else //scrolling
                        app_list_done.hide()
                }
            })

            app_list_done.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
            app_list_done.setOnLongClickListener {
                MultiPreferences(context ?: return@setOnLongClickListener false)
                        .putStringSet(Stuff.PREF_BLACKLIST, setOf())
                Stuff.toast(activity, "Cleared blacklist")
                true
            }
        }
        val excludePackageNames = getMusicPlayers(adapter)
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            val pm = activity?.packageManager ?: return@execute

            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            val launcherApps = pm.queryIntentActivities(intent, 0)

            val pkgMap = mutableMapOf<String, ResolveInfo>()
            launcherApps.forEach {
                val ai = it.activityInfo.applicationInfo
                if (ai.icon != 0 && ai.enabled)
                    pkgMap[it.activityInfo.applicationInfo.packageName] = it
            }
            if (Main.isTV) {
                intent.removeCategory(Intent.CATEGORY_LAUNCHER)
                intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                val tvApps = pm.queryIntentActivities(intent, 0)
                tvApps.forEach {
                    if (!pkgMap.containsKey(it.activityInfo.applicationInfo.packageName) &&
                            it.activityInfo.applicationInfo.icon != 0 &&
                            it.activityInfo.applicationInfo.enabled)
                        pkgMap[it.activityInfo.applicationInfo.packageName] = it
                }
            }

            adapter.addSectionHeader(getString(R.string.video_players))

            Stuff.IGNORE_ARTIST_META.forEach {
                if (pkgMap.containsKey(it))
                    adapter.add(pkgMap.remove(it)!!.activityInfo.applicationInfo, firstRun)
            }

            excludePackageNames.forEach {
                pkgMap.remove(it)
            }

            if (firstRun) {
                val prefs = MultiPreferences(context ?: return@execute)
                val wSet = mutableSetOf<String>()
                wSet.addAll(adapter.getSelectedPackages())
                prefs.putStringSet(Stuff.PREF_WHITELIST, wSet)
                if (Main.isTV)
                    prefs.putBoolean(Stuff.PREF_AUTO_DETECT, false)
            }
            adapter.addSectionHeader(getString(R.string.other_apps))

            val otherApps = pkgMap.values.toMutableList()
            Collections.sort(otherApps, ResolveInfo.DisplayNameComparator(activity!!.packageManager))
            otherApps.forEach {
                adapter.add(it.activityInfo.applicationInfo)
            }

            val oldCount = adapter.itemCount

            appListLoaded = true
            app_list?.post {
                adapter.notifyItemRangeChanged(oldCount-1, adapter.itemCount, 0)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.action_app_list)
    }
    override fun onStop() {
        val prefs = MultiPreferences(context ?: return)
        if (firstRun)
            prefs.putBoolean(Stuff.PREF_ACTIVITY_FIRST_RUN, false)
        if (app_list != null) {
            val wSet = mutableSetOf<String>()

            val adapter = app_list.adapter as AppListAdapter
            wSet.addAll(adapter.getSelectedPackages())

            //BL = old WL - new WL
            val bSet = prefs.getStringSet(Stuff.PREF_BLACKLIST, setOf()) +
                    prefs.getStringSet(Stuff.PREF_WHITELIST, setOf()) -
                    wSet

            prefs.putStringSet(Stuff.PREF_WHITELIST, wSet)
            prefs.putStringSet(Stuff.PREF_BLACKLIST, bSet)
        }
        super.onStop()
    }


    private fun getMusicPlayers(adapter: AppListAdapter): MutableSet<String> {
        val prefs = MultiPreferences(context!!)
        firstRun = prefs.getBoolean(Stuff.PREF_ACTIVITY_FIRST_RUN, true)

        val pm = activity!!.packageManager
        val intent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER)
        //the newer intent category doesn't catch many players including poweramp
        val musicPlayers = pm.queryIntentActivities(intent, 0)
        val excludePackageNames = mutableSetOf<String>(activity!!.packageName)

        adapter.addSectionHeader(getString(R.string.music_players))
        musicPlayers.forEach {
            if (!excludePackageNames.contains(it.activityInfo.packageName)) {
                adapter.add(it.activityInfo.applicationInfo, firstRun)
                excludePackageNames.add(it.activityInfo.packageName)
            }
        }
        adapter.notifyDataSetChanged()
        return excludePackageNames
    }
}