package com.arn.scrobble

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class EditActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blank)

        if (savedInstanceState == null) {
            val ef = EditFragment()
            ef.arguments = intent.extras

            supportFragmentManager!!.beginTransaction()
                    .replace(R.id.blank_frame, ef)
                    .commit()
        }
    }
}