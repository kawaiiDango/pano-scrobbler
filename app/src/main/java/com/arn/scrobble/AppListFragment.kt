package com.arn.scrobble

import android.app.Fragment
import android.content.Intent
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
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import java.util.*


/**
 * Created by arn on 05/09/2017.
 */
class AppListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity.findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout).title = getString(R.string.action_app_list)
        val vg = activity.findViewById<AppBarLayout>(R.id.app_bar)
        vg.setExpanded(false, true)
//        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.content_app_list, container, false)
    }

    lateinit private var adapter: AppListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ab = (activity as AppCompatActivity).supportActionBar ?: return
        ab.setDisplayHomeAsUpEnabled(false)
        val appList = activity.findViewById<ListView>(R.id.app_list)
        adapter = AppListAdapter(activity, R.layout.list_item_app, R.layout.list_header)
        appList.adapter = adapter
        getAppList()

        appList.setOnItemClickListener{
            adapterView: AdapterView<*>, view1: View, i: Int, l: Long ->
            Stuff.log(activity,"clicked $i, $l")
        }
    }

    private fun getAppList(){
        val pm = activity.packageManager
        val resolveIntent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.withAppendedPath(
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI, "1")
        resolveIntent.setDataAndType(uri, "audio/*")
//        val uri = Uri.withAppendedPath(
//                MediaStore.Video.Media.INTERNAL_CONTENT_URI, "1")
//        resolveIntent.setDataAndType(uri, "video/*")

        val otherApps = pm.getInstalledApplications(PackageManager.GET_META_DATA) as MutableList<ApplicationInfo>
        val potentialApps = pm.queryIntentActivities(resolveIntent, PackageManager.GET_RESOLVED_FILTER)
        val excludePackageNames = mutableListOf<String>(activity.packageName)

        adapter.addSectionHeader("Music Players")
        potentialApps.forEach{
            adapter.add(it.activityInfo.applicationInfo)
            excludePackageNames.add(it.activityInfo.packageName)
        }
        adapter.addSectionHeader("Video Streaming Apps")
        otherApps.forEach {
            if(Stuff.APPS_IGNORE_ARTIST_META.contains(it.packageName)) {
                adapter.add(it)
                excludePackageNames.add(it.packageName)
            }
        }

        val iterator = otherApps.iterator()
        while (iterator.hasNext()) {
            val applicationInfo = iterator.next()
            if (excludePackageNames.contains(applicationInfo.packageName) || applicationInfo.icon == 0) {
                iterator.remove()
            }
        }

        adapter.addSectionHeader("Other Apps")
        Collections.sort(otherApps, ApplicationInfo.DisplayNameComparator(pm))

        adapter.addAll(otherApps)
    }
}