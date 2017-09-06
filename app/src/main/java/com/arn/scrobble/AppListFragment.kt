package com.arn.scrobble

import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import java.util.*


/**
 * Created by arn on 05/09/2017.
 */
class AppListFragment : Fragment() {
    private lateinit var prefs:SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity.findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)?.title = getString(R.string.action_app_list)
        val vg = activity.findViewById<AppBarLayout>(R.id.app_bar)
        vg?.setExpanded(false, true)
//        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.content_app_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ab = (activity as AppCompatActivity).supportActionBar ?: return
        ab.setDisplayHomeAsUpEnabled(false)
        val appList = view.findViewById<ListView>(R.id.app_list)
        val adapter = AppListAdapter(activity, R.layout.list_item_app, R.layout.header_default)
        appList.adapter = adapter
        getAppList(adapter)

        appList.setOnItemClickListener{
            adapterView: AdapterView<*>, view1: View, pos1: Int, l: Long ->
            val i=l.toInt()
            adapterView as ListView

            adapterView.setItemChecked(i, adapterView.isItemChecked(i)) //i have no idea why this works
            adapter.notifyDataSetChanged()
        }
    }

    override fun onStop() {
        super.onStop()
        val appList = activity.findViewById<ListView>(R.id.app_list)
        if (appList != null) {
            val prefsSet = mutableSetOf<String>()
            appList.checkedItemIds.forEach {
                prefsSet.add((appList.adapter.getItem(it.toInt()) as ApplicationInfo).packageName)
            }
            prefs.edit().putStringSet(Stuff.APP_LIST_PREFS, prefsSet).apply()
        }
    }

    private fun getAppList(adapter: AppListAdapter){
        prefs = activity.getSharedPreferences(Stuff.APP_LIST_PREFS, Context.MODE_PRIVATE)
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
            adapter.add(it.activityInfo.applicationInfo)
            excludePackageNames.add(it.activityInfo.packageName)
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
        Collections.sort(otherApps, ApplicationInfo.DisplayNameComparator(pm))

        adapter.addAll(otherApps)
    }
}