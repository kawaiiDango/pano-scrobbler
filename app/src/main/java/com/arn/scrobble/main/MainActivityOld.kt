package com.arn.scrobble.main

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.FloatingWindow
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.databinding.ContentMainBinding
import com.arn.scrobble.databinding.HeaderNavBinding
import com.arn.scrobble.search.IndexingWorker
import com.arn.scrobble.utils.LocaleUtils.setLocaleCompat
import com.arn.scrobble.utils.NavUtils
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.dp
import com.arn.scrobble.utils.UiUtils.fadeToolbarTitle
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException


class MainActivityOld : AppCompatActivity(),
    NavController.OnDestinationChangedListener {

    private val mainPrefs = PlatformStuff.mainPrefs
    lateinit var binding: ContentMainBinding
    private val billingViewModel by viewModels<BillingViewModel>()
    private val mainNotifierViewModel by viewModels<MainViewModel>()
    private lateinit var navController: NavController
    private var navHeaderBinding: HeaderNavBinding? = null
    private lateinit var mainFab: View

    override fun onCreate(savedInstanceState: Bundle?) {
        var canShowNotices = false

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        UiUtils.isTabletUi = resources.getBoolean(R.bool.is_tablet_ui)

        binding = ContentMainBinding.inflate(layoutInflater)

        if (UiUtils.isTabletUi) {
            navHeaderBinding = HeaderNavBinding.inflate(layoutInflater, binding.sidebarNav, false)

            if (mainNotifierViewModel.isItChristmas)
                UiUtils.applySnowfall(
                    navHeaderBinding!!.navProfilePic,
                    navHeaderBinding!!.root,
                    layoutInflater,
                    lifecycleScope
                )

            mainFab = ExtendedFloatingActionButton(this).apply {
                id = R.id.main_extended_fab
                val lp = CoordinatorLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.sidebar_width) - 2 * resources.getDimensionPixelSize(
                        R.dimen.fab_margin
                    ),
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT
                )
                lp.insetEdge = Gravity.TOP
                lp.gravity = Gravity.TOP or Gravity.START
                lp.setMargins(resources.getDimensionPixelSize(R.dimen.fab_margin))
                layoutParams = lp
                visibility = View.GONE
                binding.root.addView(this)
            }
            binding.sidebarNav.addHeaderView(navHeaderBinding!!.root)
            binding.sidebarNav.visibility = View.VISIBLE
        } else {
            mainFab = FloatingActionButton(this).apply {
                id = R.id.main_fab
                val lp = CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                    CoordinatorLayout.LayoutParams.WRAP_CONTENT
                )
                lp.dodgeInsetEdges = Gravity.BOTTOM or Gravity.END
                lp.anchorGravity = Gravity.BOTTOM or Gravity.END
                lp.anchorId = R.id.nav_host_fragment
                setupInsets(
                    additionalSpaceBottom = 8.dp,
                    additionalSpaceSides = 16.dp,
                    addBottomNavHeight = false
                )
                visibility = View.INVISIBLE
                layoutParams = lp
                binding.root.addView(this)
            }
            binding.sidebarNav.visibility = View.GONE
        }

        if (Stuff.isEdgeToEdge) {
            binding.root.fitsSystemWindows = true
            binding.appBar.fitsSystemWindows = true
            binding.heroDarkOverlayTop.isVisible = true
//            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        setContentView(binding.root)

        navController = binding.navHostFragment.getFragment<NavHostFragment>().navController

        if (Stuff.isLoggedIn()) {
            canShowNotices = true

            // todo make async and move to viewmodel
            runBlocking {
                Scrobblables.current.filterNotNull().first().let {
                    mainNotifierViewModel.initializeCurrentUser(it.userAccount.user)
                }
            }
            handleIntent(intent, false)
        }

        val appBarConfiguration = AppBarConfiguration(navController.graph)
        binding.ctl.setupWithNavController(binding.toolbar, navController, appBarConfiguration)

        navHeaderBinding?.let {
            NavUtils.setProfileSwitcher(it, navController, mainNotifierViewModel)
        }

        navController.addOnDestinationChangedListener(this)


        // hide back button on tv
        if (Stuff.isTv) {
            binding.ctl.updateLayoutParams<AppBarLayout.LayoutParams> {
                scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
            }
            binding.toolbar.isVisible = false
        }

//        collectLatestLifecycleFlow(mainNotifierViewModel.fabData) {
//            // onDestroy of previous fragment gets called AFTER on create of the current fragment
//            it ?: return@collectLatestLifecycleFlow
//
//            it.lifecycleOwner.lifecycle.addObserver(
//                object : LifecycleEventObserver {
//                    override fun onStateChanged(
//                        source: LifecycleOwner,
//                        event: Lifecycle.Event
//                    ) {
//                        when (event) {
//                            Lifecycle.Event.ON_DESTROY -> {
//                                source.lifecycle.removeObserver(this)
//
//                                if (mainNotifierViewModel.fabData.value?.lifecycleOwner == source)
//                                    hideFab()
//                            }
//
//                            else -> {}
//                        }
//                    }
//                }
//            )
//
//            if (UiUtils.isTabletUi) {
//                (mainFab as ExtendedFloatingActionButton).apply {
//                    setIconResource(it.iconRes)
//                    setText(it.stringRes)
//                }
//            } else {
//                (mainFab as FloatingActionButton).apply {
//                    setImageResource(it.iconRes)
//                    contentDescription = getString(it.stringRes)
//                }
//            }
//            mainFab.setOnClickListener(it.clickListener)
//
//            if (BuildConfig.DEBUG) {
//                mainFab.setOnLongClickListener(it.longClickListener)
//                mainFab.isLongClickable = it.longClickListener != null
//            }
//            showHiddenFab()
//        }

        collectLatestLifecycleFlow(mainNotifierViewModel.canIndex) {
            if (!BuildConfig.DEBUG)
                binding.sidebarNav.menu.findItem(R.id.nav_do_index)?.isVisible = it

            val lastMaxIndexTime = mainPrefs.data.map { it.lastMaxIndexTime }.first()

            if (it && lastMaxIndexTime != null) {
                IndexingWorker.schedule(this)
            }
        }

        if (canShowNotices) {
            lifecycleScope.launch {
                showChangelogIfNeeded()
            }
        }

        collectLatestLifecycleFlow(mainNotifierViewModel.drawerData) {
            NavUtils.updateHeaderWithDrawerData(
                navHeaderBinding ?: return@collectLatestLifecycleFlow,
                mainNotifierViewModel
            )
        }

        collectLatestLifecycleFlow(Stuff.globalExceptionFlow) { e ->
            if (BuildConfig.DEBUG)
                e.printStackTrace()

            if (e is ApiException && e.code != 504) { // suppress cache not found exceptions
                Snackbar.make(
                    binding.root,
                    e.localizedMessage ?: e.message ?: "Error",
                    Snackbar.LENGTH_SHORT
                ).apply { if (!UiUtils.isTabletUi) anchorView = binding.bottomNav }
                    .show()
            }

            if (e is SerializationException) {
                Logger.w(e.cause) { "SerializationException" }
            }
        }

        billingViewModel.queryPurchasesAsync()
    }

    override fun onDestroy() {
        mainNotifierViewModel.prevDestinationId = null
        super.onDestroy()
    }

    private suspend fun showChangelogIfNeeded() {
        val changelogHashcode = getString(R.string.changelog_text).hashCode()
        val storedHashcode = mainPrefs.data.map { it.changelogSeenHashcode }.first()

        if (storedHashcode != changelogHashcode) {
            if (storedHashcode != null) {
                navController.navigate(R.id.changelogDialogFragment)
            }
            // else is fresh install
            mainPrefs.updateData { it.copy(changelogSeenHashcode = changelogHashcode) }
        }
    }

    fun hideFab(removeListeners: Boolean = true) {
        if (UiUtils.isTabletUi) {
            (mainFab as ExtendedFloatingActionButton).hide()
            binding.sidebarNav.updateLayoutParams<MarginLayoutParams> {
                topMargin = 0
            }
        } else {
            (mainFab as FloatingActionButton).hide()
        }

        if (removeListeners) {
            mainNotifierViewModel.setFabData(null)
            mainFab.setOnClickListener(null)
        }
    }

    private fun showHiddenFab() {
//        if (mainNotifierViewModel.fabData.value == null) return

        if (UiUtils.isTabletUi) {
            (mainFab as ExtendedFloatingActionButton).show()
            binding.sidebarNav.updateLayoutParams<MarginLayoutParams> {
                topMargin = resources.getDimensionPixelSize(R.dimen.fab_margin)
            }
        } else {
            (mainFab as FloatingActionButton).show()
        }
    }

    private fun handleIntent(intent: Intent, onNewIntent: Boolean) {
        if (Stuff.isLoggedIn()) {
            if (intent.categories?.contains(Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES) == true) {
                navController.navigate(R.id.prefFragment)
            } else if (intent.action == Intent.ACTION_SEARCH) {
                navController.navigate(R.id.searchFragment, intent.extras)
            } else if (onNewIntent) {
                navController.handleDeepLink(intent)
            }
        }
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        val showBottomNavOn = setOf(
            R.id.myHomePagerFragment,
            R.id.othersHomePagerFragment,
            R.id.chartsPagerFragment,
            R.id.infoExtraFullFragment
        )

        if (destination !is FloatingWindow && destination.id !in showBottomNavOn) {
            binding.appBar.expandTillToolbar()
        }

        fadeToolbarTitle(binding.ctl)

        destination.arguments[Stuff.ARG_TITLE]?.let {
            binding.ctl.title = it.defaultValue as String
        }

        mainNotifierViewModel.prevDestinationId = destination.id
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent, true)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase ?: return)
        setLocaleCompat()
    }

    // todo remove
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        Logger.d { "focus: $currentFocus" }
        return super.onKeyUp(keyCode, event)
    }

    // https://stackoverflow.com/a/28939113/1067596
// EditText, clear focus on touch outside
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    ContextCompat.getSystemService(this, InputMethodManager::class.java)
                        ?.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }
}
