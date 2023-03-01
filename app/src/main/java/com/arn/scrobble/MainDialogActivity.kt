package com.arn.scrobble

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.arn.scrobble.LocaleUtils.setLocaleCompat
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.themes.ColorPatchUtils

class MainDialogActivity : AppCompatActivity() {
    private val billingViewModel by viewModels<BillingViewModel>()
    private val activityViewModel by viewModels<MainNotifierViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        ColorPatchUtils.setTheme(this, billingViewModel.proStatus.value == true)

        setContentView(R.layout.content_main_dialog)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        activityViewModel.pushUser(Scrobblables.current?.userAccount?.user ?: return)

        val graph =
            navHostFragment.navController.navInflater.inflate(R.navigation.nav_graph_dialog_activity)
        navHostFragment.navController.setGraph(graph, null)

        navHostFragment.navController.addOnDestinationChangedListener { navController, navDestination, args ->
            if (activityViewModel.prevDestinationId != null && navDestination.id == R.id.emptyFragment)
                finish()
            activityViewModel.prevDestinationId = navDestination.id
        }

        // from widget
        if (intent.getBooleanExtra(Stuff.ARG_INFO, false)
            && navHostFragment.navController.currentDestination?.id != R.id.infoFragment
        )
            navHostFragment.navController.navigate(R.id.infoFragment, intent.extras)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase ?: return)
        setLocaleCompat()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        findNavController(R.id.nav_host_fragment).handleDeepLink(intent)
    }

}