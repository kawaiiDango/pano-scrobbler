package com.arn.scrobble.main

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.arn.scrobble.R
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.themes.ColorPatchUtils
import com.arn.scrobble.utils.LocaleUtils.setLocaleCompat
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.myHash
import java.util.Objects

class MainDialogActivity : AppCompatActivity() {
    private val billingViewModel by viewModels<BillingViewModel>()
    private val activityViewModel by viewModels<MainNotifierViewModel>()
    private lateinit var navHostFragment: NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        ColorPatchUtils.setTheme(this, billingViewModel.proStatus.value)

        setContentView(R.layout.content_main_dialog)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        activityViewModel.initializeCurrentUser(Scrobblables.currentScrobblableUser ?: return)

        navHostFragment.navController.addOnDestinationChangedListener { navController, navDestination, args ->
            if (activityViewModel.prevDestinationId != null && navDestination.id == R.id.emptyDialogFragment)
                finish()
            activityViewModel.prevDestinationId = navDestination.id
        }

        // navigate
        if (savedInstanceState == null) {
            navigate(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        activityViewModel.prevDestinationId = null
        navHostFragment.navController.popBackStack(
            navHostFragment.navController.graph.startDestinationId,
            false
        )
        navigate(intent)
    }

    private fun navigate(intent: Intent) {
        val destinationId = intent.getIntExtra(ARG_DESTINATION, 0)
        if (destinationId != 0) {
            navHostFragment.navController.navigate(
                destinationId,
                intent.getBundleExtra(ARG_NAV_ARGS),
            )
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase ?: return)
        setLocaleCompat()
    }

    companion object {
        const val ARG_DESTINATION = "@destination"
        const val ARG_NAV_ARGS = "@nav_args"

        private fun createDestinationIntent(@IdRes destinationId: Int, args: Bundle) =
            Intent(App.context, MainDialogActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(ARG_DESTINATION, destinationId)
                putExtra(ARG_NAV_ARGS, args)
            }

        // use this instead of NavDeepLinkBuilder to avoid clearing the current task and appear floating
        fun createDestinationPendingIntent(
            @IdRes destinationId: Int,
            args: Bundle,
            mutable: Boolean = false
        ) =
            PendingIntent.getActivity(
                App.context,
                Objects.hash(args.myHash(), destinationId),
                createDestinationIntent(destinationId, args),
                if (mutable) Stuff.updateCurrentOrMutable else Stuff.updateCurrentOrImmutable
            )!!

    }

}