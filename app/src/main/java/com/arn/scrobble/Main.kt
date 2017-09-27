package com.arn.scrobble

import android.app.Fragment
import android.app.Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.arn.scrobble.pref.AppListFragment
import com.arn.scrobble.pref.PrefFragment


class Main : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val ctl = findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)
        ctl.tag = " "
        val abl = findViewById<AppBarLayout>(R.id.app_bar)
        abl.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            var scrollRange = -1

            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.totalScrollRange
                }
                if (scrollRange + verticalOffset == 0) {
                    val f: Fragment? = fragmentManager.findFragmentByTag(Stuff.GET_RECENTS)
                    if (f != null && f.isVisible) {
                        ctl.title = getString(R.string.app_name)
                    }
                    heroExpanded = true
                } else if (heroExpanded) {
                    ctl.title = ctl.tag as CharSequence
                    heroExpanded = false
                }
            }
        })

        if( FirstThingsFragment.checkAuthTokenExists(this) &&
                FirstThingsFragment.checkNLAccess(this)) {
            val deepLinkExtra = intent?.getIntExtra(Stuff.DEEP_LINK_KEY, 0) ?: 0

            val recentsFragment = RecentsFragment()
            fragmentManager.beginTransaction()
                    .replace(R.id.frame, recentsFragment, Stuff.GET_RECENTS)
                    .commit()
            if (deepLinkExtra == Stuff.DL_SETTINGS || intent?.categories?.contains(INTENT_CATEGORY_NOTIFICATION_PREFERENCES) == true)
                fragmentManager.beginTransaction()
                        .hide(recentsFragment)
                        .add(R.id.frame, PrefFragment())
                        .addToBackStack(null)
                        .commit()
            else if (deepLinkExtra == Stuff.DL_APP_LIST)
                fragmentManager.beginTransaction()
                        .hide(recentsFragment)
                        .add(R.id.frame, AppListFragment())
                        .addToBackStack(null)
                        .commit()
        } else {
            fragmentManager.beginTransaction()
                    .replace(R.id.frame, FirstThingsFragment())
                    .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
    }

    companion object {
        var heroExpanded = false
    }
}
