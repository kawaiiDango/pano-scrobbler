package com.arn.scrobble

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class EditActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val ef = EditFragment()
            ef.arguments = intent.extras
            supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, ef)
                    .commit()
        }
    }
}