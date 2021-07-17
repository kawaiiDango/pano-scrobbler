package com.arn.scrobble.edits

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arn.scrobble.R
import com.arn.scrobble.VMFactory
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.themes.ColorPatchUtils

class EditDialogActivity: AppCompatActivity() {
    private val billingViewModel by lazy { VMFactory.getVM(this, BillingViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (billingViewModel.proStatus.value == true)
            ColorPatchUtils.setTheme(this)
        else
            theme.applyStyle(R.style.ColorPatch_Pink_Main, true)

        super.onCreate(savedInstanceState)

//        if (savedInstanceState == null) {
            val ef = EditDialogFragment()
            ef.arguments = intent.extras
            ef.show(supportFragmentManager, null)
//        }
    }
}