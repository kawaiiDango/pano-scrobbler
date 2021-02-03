package com.arn.scrobble

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class EditActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        theme.applyStyle(R.style.ColorPatch_Pink, true)
        super.onCreate(savedInstanceState)

//        if (savedInstanceState == null) {
            val ef = EditFragment()
            ef.arguments = intent.extras
            ef.show(supportFragmentManager, null)
//        }
    }
}