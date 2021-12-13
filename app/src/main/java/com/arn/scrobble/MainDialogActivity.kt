package com.arn.scrobble

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arn.scrobble.LocaleUtils.getLocaleContextWrapper
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.edits.BlockedMetadataAddDialogFragment
import com.arn.scrobble.edits.EditDialogFragment
import com.arn.scrobble.themes.ColorPatchUtils

class MainDialogActivity : AppCompatActivity() {
    private val billingViewModel by lazy { VMFactory.getVM(this, BillingViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        ColorPatchUtils.setTheme(this, billingViewModel.proStatus.value == true)

        super.onCreate(savedInstanceState)

        val dialogFragment = when {
            intent.extras?.getParcelable(Stuff.ARG_DATA) as? BlockedMetadata != null -> {
                BlockedMetadataAddDialogFragment()
            }
            else -> {
                EditDialogFragment()
            }
        }

        val transaction = supportFragmentManager.beginTransaction().addToBackStack(null)

        dialogFragment.apply {
            arguments = intent.extras
            show(transaction, null)
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0)
                finish()
        }
//        if (savedInstanceState == null) {
//        }
    }

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null)
            super.attachBaseContext(newBase.getLocaleContextWrapper())
    }

}