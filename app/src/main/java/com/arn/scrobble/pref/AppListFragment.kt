package com.arn.scrobble.pref

import android.app.Fragment
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import kotlinx.android.synthetic.main.content_app_list.*
import java.util.Collections
import android.widget.AbsListView


/**
 * Created by arn on 05/09/2017.
 */
class AppListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.content_app_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = AppListAdapter(activity, R.layout.list_item_app, R.layout.header_default)
        app_list.adapter = adapter
        app_list.setOnScrollListener(object : AbsListView.OnScrollListener {
            var lastFirstVisibleItem:Int = 0

            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                if(scrollState == 0) { //scrolling stopped
                    app_list_done.show()
                } else //scrolling
                    app_list_done.hide()
/*
                if (view.id == app_list.id) {
                    val currentFirstVisibleItem = app_list.firstVisiblePosition
                    if (currentFirstVisibleItem > lastFirstVisibleItem) { //scrolling down
                        app_list_done.hide()
                    } else if (currentFirstVisibleItem < lastFirstVisibleItem) {//scrolling up
                        app_list_done.show()
                    }

                    lastFirstVisibleItem = currentFirstVisibleItem
                }
*/
            }

            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
            }
        })

        app_list_done.setOnClickListener {
            fragmentManager.popBackStack()
        }
        val otherApps = getAppList(adapter)
        Thread({
            Collections.sort(otherApps, ApplicationInfo.DisplayNameComparator(activity.packageManager))
            app_list?.post({
                val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
                val firstRun = prefs.getBoolean(Stuff.FIRST_RUN_PREF, true)
                if (firstRun){
                    for(i in 0 until app_list.count)
                        app_list.setItemChecked(i, true)
                    prefs.edit().putBoolean(Stuff.FIRST_RUN_PREF, false).apply()
                }
                otherApps.forEach {adapter.add(it)}
            })
        }).start()

        app_list.setOnItemClickListener{
            adapterView: AdapterView<*>, view1: View, pos1: Int, l: Long ->
            val i=l.toInt()
            adapterView as ListView
            if (adapterView.checkedItemCount > Stuff.MAX_APPS){
                Stuff.toast(activity, getString(R.string.max_apps_exceeded, Stuff.MAX_APPS))
                adapterView.setItemChecked(i, false)
            } else {
                adapterView.setItemChecked(i, adapterView.isItemChecked(i)) //i have no idea why this works
            }
            adapter.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        Stuff.setTitle(activity, R.string.action_app_list)
    }
    override fun onStop() {
        super.onStop()
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        if (app_list != null) {
            val wSet = mutableSetOf<String>()
            val bSet = prefs.getStringSet(Stuff.APP_BLACKLIST, mutableSetOf())
            app_list.checkedItemIds.forEach {
                val packageName = (app_list.adapter.getItem(it.toInt()) as ApplicationInfo).packageName ?: return@forEach
                wSet.add(packageName)
            }
            bSet.removeAll(wSet)
            prefs.edit()
                    .putStringSet(Stuff.APP_WHITELIST, wSet)
                    .putStringSet(Stuff.APP_BLACKLIST,  bSet)
                    .apply()
        }
    }

    private fun getAppList(adapter: AppListAdapter): MutableList<ApplicationInfo> {
        val pm = activity.packageManager
        val resolveIntent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.withAppendedPath(
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI, "1")
        resolveIntent.setDataAndType(uri, "audio/*")

        val otherApps = pm.getInstalledApplications(PackageManager.GET_META_DATA) as MutableList<ApplicationInfo>
        val potentialApps = pm.queryIntentActivities(resolveIntent, PackageManager.GET_RESOLVED_FILTER)
        val excludePackageNames = mutableListOf<String>(activity.packageName)

        adapter.addSectionHeader(getString(R.string.music_players))
        potentialApps.forEach{
            if (!excludePackageNames.contains(it.activityInfo.packageName)) {
                adapter.add(it.activityInfo.applicationInfo)
                excludePackageNames.add(it.activityInfo.packageName)
            }

        }
        adapter.addSectionHeader(getString(R.string.video_players))
        otherApps.forEach {
            if(Stuff.APPS_IGNORE_ARTIST_META.contains(it.packageName)) {
                adapter.add(it)
                excludePackageNames.add(it.packageName)
            }
        }

        val it = otherApps.iterator()
        while (it.hasNext()) {
            val applicationInfo = it.next()
            if (excludePackageNames.contains(applicationInfo.packageName) ||
                    applicationInfo.icon == 0 ||
                    (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                it.remove()
            }
        }

        adapter.addSectionHeader(getString(R.string.other_apps))

        return otherApps
    }
}