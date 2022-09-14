package com.arn.scrobble

import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.viewpager.widget.ViewPager
import com.arn.scrobble.friends.UserSerializable.Companion.toUserSerializable
import com.arn.scrobble.pref.MainPrefs
import com.google.android.material.tabs.TabLayout


class HomePagerFragment : PagerBaseFragment(), ViewPager.OnPageChangeListener {

    private var backStackChecked = false
    private val prefs by lazy { MainPrefs(context!!) }
    private val viewModel by viewModels<HomePagerVM>()
    private val activityViewModel by viewModels<MainNotifierViewModel>({ activity!! })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.onClearedCallback = {
            if (activityViewModel.userStackDepth > 1)
                activityViewModel.popUser()
        }

        tabMeta = arrayOf(
            R.string.scrobbles to R.drawable.vd_history,
            R.string.loved to R.drawable.vd_heart,
            R.string.friends to R.drawable.vd_friends,
            R.string.charts to R.drawable.vd_charts
        )
        adapter = HomePagerAdapter(childFragmentManager)

        viewModel.userInfo.observe(viewLifecycleOwner) { user ->
            user ?: return@observe
            activityViewModel.pushUser(user.toUserSerializable())
            // todo fix after nav components
        }

        if (activityViewModel.userStackDepth == 1)
            setGestureExclusions(true)
        else {
            // opened VIEW link
            // todo: reimplement
//            if (arguments!!.getLong(Stuff.ARG_REGISTERED_TIME) == 0L)
//                viewModel.fetchUserInfo(arguments!!.getString(Stuff.ARG_USERNAME)!!)
        }
        if (savedInstanceState == null)
            backStackChecked = false
        (view as ViewPager).addOnPageChangeListener(this)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        setGestureExclusions(false)
        super.onDestroyView()
    }

    fun setGestureExclusions(set: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !resources.getBoolean(R.bool.is_rtl)
        ) {
            val list = if (set)
                listOf(Rect(0, 0, 100, resources.displayMetrics.heightPixels))
            else
                listOf()
            view?.systemGestureExclusionRects = list
            //fuck gestures
        }
    }

    override fun onStart() {
        if (!backStackChecked) { //dont invoke if coming from background/app switching
            val lastTab = if (arguments != null && arguments!!.getInt(Stuff.ARG_TYPE, -1) != -1)
                arguments!!.getInt(Stuff.ARG_TYPE)
            else if (activityViewModel.userStackDepth > 1)
                (view as ViewPager).currentItem
            else if (activity!!.intent?.getIntExtra(Stuff.DIRECT_OPEN_KEY, 0) == Stuff.DL_RECENTS)
                0
            else if (activity!!.intent?.getIntExtra(Stuff.DIRECT_OPEN_KEY, 0) == Stuff.DL_CHARTS)
                3
            else
                prefs.lastHomePagerTab
            initTabs(lastTab)
            (activity as MainActivity).onBackStackChanged()
            backStackChecked = true
        }
        super.onStart()
    }

    override fun onDestroy() {
        if (_binding != null)
            binding.pager.removeOnPageChangeListener(this)
        super.onDestroy()
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        (activity as MainActivity).onBackStackChanged()
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        super.onTabSelected(tab)
        if (activity!!.intent?.getIntExtra(Stuff.DIRECT_OPEN_KEY, 0) == 0 &&
            activityViewModel.userStackDepth == 1
        ) {
            context ?: return
            prefs.lastHomePagerTab = tab.position
        }
    }
}