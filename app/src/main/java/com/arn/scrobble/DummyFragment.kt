package com.arn.scrobble

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment


/**
 * Created by arn on 06/09/2017.
 */
class DummyFragment: Fragment(R.layout.content_avd_test) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val avd = view.findViewById<ImageView>(R.id.test_avd).drawable as AnimatedVectorDrawable
        Handler().postDelayed({ avd.start() }, 1000)
    }

    override fun onStart() {
        super.onStart()

    }

}