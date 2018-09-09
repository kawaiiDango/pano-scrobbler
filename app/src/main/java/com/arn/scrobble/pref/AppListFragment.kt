package com.arn.scrobble.pref

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.transition.Fade
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import kotlinx.android.synthetic.main.content_app_list.*
import java.util.Collections


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

        val adapter = AppListAdapter(activity!!)
        app_list.layoutManager = LinearLayoutManager(context)
        app_list.adapter = adapter
        app_list.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(view: RecyclerView, scrollState: Int) {
                if(scrollState == 0) { //scrolling stopped
                    app_list_done.show()
                } else //scrolling
                    app_list_done.hide()
            }
        })

        app_list_done.setOnClickListener {
            activity!!.supportFragmentManager.popBackStack()
        }
        app_list_done.setOnLongClickListener {
            MultiPreferences(context?: return@setOnLongClickListener false )
                    .putStringSet(Stuff.PREF_BLACKLIST, setOf())
            Stuff.toast(activity, "Cleared blacklist")
            true
        }
        val excludePackageNames = getAppList(adapter)
        Thread {
            val pm = activity!!.packageManager
            val otherApps = pm.getInstalledApplications(PackageManager.GET_META_DATA) as MutableList<ApplicationInfo>

            adapter.addSectionHeader(getString(R.string.video_players))

            val it = otherApps.iterator()
            while (it.hasNext()) {
                val applicationInfo = it.next()
                if(Stuff.IGNORE_ARTIST_META.contains(applicationInfo.packageName)) {
                    adapter.add(applicationInfo, firstRun)
                    excludePackageNames.add(applicationInfo.packageName)
                }
                if (excludePackageNames.contains(applicationInfo.packageName) ||
                        applicationInfo.icon == 0 || !applicationInfo.enabled ||
                        (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                    it.remove()
                }
            }
            adapter.addSectionHeader(getString(R.string.other_apps))
            Collections.sort(otherApps, ApplicationInfo.DisplayNameComparator(activity!!.packageManager))

            val oldCount = adapter.itemCount
            otherApps.forEach {adapter.add(it)}
            appListLoaded = true
            app_list?.post {
                adapter.notifyItemRangeChanged(oldCount-1, adapter.itemCount, 0)
            }
        }.start()
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.action_app_list)
    }
    override fun onStop() {
        val prefs = MultiPreferences(context ?: return)
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


    private fun getAppList(adapter: AppListAdapter): MutableList<String> {
        val prefs = MultiPreferences(context!!)
        firstRun = prefs.getBoolean(Stuff.PREF_ACTIVITY_FIRST_RUN, true)
        if (firstRun)
            prefs.putBoolean(Stuff.PREF_ACTIVITY_FIRST_RUN, false)

        val pm = activity!!.packageManager
        val resolveIntent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.withAppendedPath(
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI, "1")
        resolveIntent.setDataAndType(uri, "audio/*")

        val potentialApps = pm.queryIntentActivities(resolveIntent, PackageManager.GET_RESOLVED_FILTER)
        val excludePackageNames = mutableListOf<String>(activity!!.packageName)

        adapter.addSectionHeader(getString(R.string.music_players))
        potentialApps.forEach{
            if (!excludePackageNames.contains(it.activityInfo.packageName)) {
                adapter.add(it.activityInfo.applicationInfo, firstRun)
                excludePackageNames.add(it.activityInfo.packageName)
            }

        }
        adapter.notifyDataSetChanged()
        return excludePackageNames
    }
}