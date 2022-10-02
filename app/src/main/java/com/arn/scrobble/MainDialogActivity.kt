package com.arn.scrobble

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.arn.scrobble.LocaleUtils.setLocaleCompat
import com.arn.scrobble.Stuff.getSingle
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.charts.CollageGeneratorFragment
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.edits.BlockedMetadataAddDialogFragment
import com.arn.scrobble.edits.EditDialogFragment
import com.arn.scrobble.themes.ColorPatchUtils

class MainDialogActivity : AppCompatActivity() {
    private val billingViewModel by viewModels<BillingViewModel>()
    private val activityViewModel by viewModels<MainNotifierViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        ColorPatchUtils.setTheme(this, billingViewModel.proStatus.value == true)

        val dialogFragment = when {
            intent.getSingle<BlockedMetadata>() != null -> {
                BlockedMetadataAddDialogFragment()
            }
            intent.getSingle<TimePeriod>() != null -> {
                activityViewModel.pushUser(intent.getSingle()!!)
                CollageGeneratorFragment()
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
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase ?: return)
        setLocaleCompat()
    }

}