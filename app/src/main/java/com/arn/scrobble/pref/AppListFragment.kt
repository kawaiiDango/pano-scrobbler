package com.arn.scrobble.pref

import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.provider.MediaStore
import android.transition.Fade
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.MainActivity
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ContentAppListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


/**
 * Created by arn on 05/09/2017.
 */
class AppListFragment : Fragment() {
    private var firstRun = false
    private var appListLoaded = false
    private val mainNotifierViewModel by lazy { (activity as MainActivity).mainNotifierViewModel }
    private val prefs by lazy { MainPrefs(context!!) }
    private var _binding: ContentAppListBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Fade()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        mainNotifierViewModel.backButtonEnabled = true
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainNotifierViewModel.backButtonEnabled = false

        if (!binding.appList.isInTouchMode)
            binding.appList.requestFocus()

        if (MainActivity.isTV){
            Stuff.toast(context, getString(R.string.press_back))
        }

        binding.appList.layoutManager = LinearLayoutManager(context)
        val adapter = AppListAdapter(activity!!, prefs.allowedPackages)
        binding.appList.adapter = adapter
        if (!MainActivity.isTV) {
            binding.appList.addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrollStateChanged(view: RecyclerView, scrollState: Int) {
                    if (!mainNotifierViewModel.backButtonEnabled)
                        return

                    if (scrollState == 0) { //scrolling stopped
                        binding.appListDone.show()
                    } else //scrolling
                        binding.appListDone.hide()
                }
            })

            binding.appListDone.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
            binding.appListDone.setOnLongClickListener {
                prefs.blockedPackages = setOf()
                Stuff.toast(activity, getString(R.string.cleared_disabled_apps))
                true
            }
        }
        val excludePackageNames = getMusicPlayers(adapter)
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val pm = activity?.packageManager ?: return@withContext

                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                val launcherApps = pm.queryIntentActivities(intent, 0)

                val pkgMap = mutableMapOf<String, ResolveInfo>()
                launcherApps.forEach {
                    val ai = it.activityInfo.applicationInfo
                    if (ai.icon != 0 && ai.enabled)
                        pkgMap[it.activityInfo.applicationInfo.packageName] = it
                }
                if (MainActivity.isTV) {
                    intent.removeCategory(Intent.CATEGORY_LAUNCHER)
                    intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    val tvApps = pm.queryIntentActivities(intent, 0)
                    tvApps.forEach {
                        if (!pkgMap.containsKey(it.activityInfo.applicationInfo.packageName) &&
                            it.activityInfo.applicationInfo.icon != 0 &&
                            it.activityInfo.applicationInfo.enabled
                        )
                            pkgMap[it.activityInfo.applicationInfo.packageName] = it
                    }
                }

                adapter.addSectionHeader(getString(R.string.video_players))

                Stuff.IGNORE_ARTIST_META.forEach {
                    if (pkgMap.containsKey(it))
                        adapter.add(pkgMap.remove(it)!!.activityInfo.applicationInfo, firstRun)
                }

                val browserApps = Stuff.getBrowsers(pm)
                browserApps.forEach {
                    if (pkgMap.containsKey(it.activityInfo.applicationInfo.packageName))
                        adapter.add(
                            pkgMap.remove(it.activityInfo.applicationInfo.packageName)!!.activityInfo.applicationInfo,
                            firstRun
                        )
                }

                excludePackageNames.forEach {
                    pkgMap.remove(it)
                }

                if (!isActive)
                    return@withContext

                if (firstRun) {
                    val wSet = mutableSetOf<String>()
                    wSet.addAll(adapter.getSelectedPackages())
                    prefs.allowedPackages = wSet
                    if (MainActivity.isTV)
                        prefs.autoDetectApps = false
                }
                adapter.addSectionHeader(getString(R.string.other_apps))

                val otherApps = pkgMap.values.toMutableList()
                Collections.sort(
                    otherApps,
                    ResolveInfo.DisplayNameComparator(activity!!.packageManager)
                )
                otherApps.forEach {
                    adapter.add(it.activityInfo.applicationInfo)
                }

                val oldCount = adapter.itemCount

                appListLoaded = true
                withContext(Dispatchers.Main) {
                    adapter.notifyItemRangeChanged(oldCount - 1, adapter.itemCount, 0)
                    enableNavigation()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.enabled_apps)
    }
    override fun onStop() {
        if (firstRun)
            prefs.firstRun = false
        if (_binding != null) {
            val wSet = mutableSetOf<String>()

            val adapter = binding.appList.adapter as AppListAdapter
            wSet += adapter.getSelectedPackages()

            //BL = old WL - new WL
            val bSet = prefs.blockedPackages + prefs.allowedPackages - wSet

            prefs.allowedPackages = wSet
            prefs.blockedPackages = bSet
        }
        super.onStop()
    }

    private fun enableNavigation() {
        mainNotifierViewModel.backButtonEnabled = true
        if (!MainActivity.isTV)
            binding.appListDone.show()
    }

    private fun getMusicPlayers(adapter: AppListAdapter): MutableSet<String> {
        firstRun = prefs.firstRun

        val pm = activity!!.packageManager
        val intent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER)
        //the newer intent category doesn't catch many players including poweramp
        val musicPlayers = pm.queryIntentActivities(intent, 0)
        val excludePackageNames = mutableSetOf<String>(activity!!.packageName)

        adapter.addSectionHeader(getString(R.string.music_players))
        musicPlayers.forEach {
            if (it.activityInfo.packageName !in excludePackageNames) {
                adapter.add(it.activityInfo.applicationInfo, firstRun)
                excludePackageNames.add(it.activityInfo.packageName)
            }
        }
        adapter.notifyDataSetChanged()
        return excludePackageNames
    }
}