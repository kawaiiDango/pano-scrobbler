package com.arn.scrobble

import android.app.Fragment
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Created by arn on 06/09/2017.
 */
class FirstThingsFragment: Fragment()  {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity.findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout).title = getString(R.string.first_things)
        val vg = activity.findViewById<AppBarLayout>(R.id.app_bar)
        vg.setExpanded(false, true)
        setHasOptionsMenu(false)
        return inflater.inflate(R.layout.content_first_things, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //TODO: check for noti and auth here
    }
}