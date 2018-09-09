package com.arn.scrobble

import android.os.Bundle
import android.transition.Slide
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arn.scrobble.pref.MultiPreferences


/**
 * Created by arn on 06/09/2017.
 */
class DummyFragment: Fragment() {
    private lateinit var pref: MultiPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
//        exitTransition = Slide()
//        enterTransition = Slide()
        returnTransition = Slide()
//        reenterTransition = Slide()

        val view = inflater.inflate(R.layout.content_login, container, false)

        return view
    }

    override fun onStart() {
        super.onStart()

    }

}