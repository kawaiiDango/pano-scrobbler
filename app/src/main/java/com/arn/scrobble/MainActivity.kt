package com.arn.scrobble

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.children
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.FloatingWindow
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.arn.scrobble.LocaleUtils.setLocaleCompat
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.databinding.ContentMainBinding
import com.arn.scrobble.databinding.HeaderNavBinding
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.themes.ColorPatchUtils
import com.arn.scrobble.ui.UiUtils
import com.arn.scrobble.ui.UiUtils.dp
import com.arn.scrobble.ui.UiUtils.focusOnTv
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber


class MainActivity : AppCompatActivity(),
    NavController.OnDestinationChangedListener {

    private val prefs by lazy { MainPrefs(this) }
    lateinit var binding: ContentMainBinding
    private val billingViewModel by viewModels<BillingViewModel>()
    private val mainNotifierViewModel by viewModels<MainNotifierViewModel>()
    private lateinit var navController: NavController
    private var navHeaderbinding: HeaderNavBinding? = null
    private lateinit var mainFab: View

    override fun onCreate(savedInstanceState: Bundle?) {
        Stuff.timeIt("onCreate start")

        var canShowNotices = false

        super.onCreate(savedInstanceState)

        ColorPatchUtils.setTheme(this, billingViewModel.proStatus.value == true)
        UiUtils.isTabletUi = resources.getBoolean(R.bool.is_tablet_ui)

        binding = ContentMainBinding.inflate(layoutInflater)

        if (UiUtils.isTabletUi) {
            navHeaderbinding = HeaderNavBinding.inflate(layoutInflater, binding.sidebarNav, false)
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
            binding.sidebarNav.addHeaderView(navHeaderbinding!!.root)
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

        if (Stuff.isTv) {
            binding.ctl.updateLayoutParams<AppBarLayout.LayoutParams> {
                scrollFlags =
                    scrollFlags or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
            }
        }

        setContentView(binding.root)
        Stuff.timeIt("onCreate setContentView")

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Stuff.isTv) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                binding.toolbar.children
                    .find { it is ImageButton }
                    ?.isFocusable = false
        }

        val navHostFragment = binding.navHostFragment.getFragment<NavHostFragment>()
        navController = navHostFragment.navController

        navController.navInflater.inflate(R.navigation.nav_graph).let {
            val startArguments = bundleOf(Stuff.ARG_TAB to prefs.lastHomePagerTab)

            if (savedInstanceState == null) {
                if (Stuff.isLoggedIn()) {
                    canShowNotices = true
                    mainNotifierViewModel.currentUser = Scrobblables.currentScrobblableUser!!
                } else {
                    it.setStartDestination(R.id.onboardingFragment)
                }
            }
            navController.setGraph(it, startArguments)
        }

        val appBarConfiguration = AppBarConfiguration(navController.graph)
        binding.ctl.setupWithNavController(binding.toolbar, navController, appBarConfiguration)

        navHeaderbinding?.let {
            NavUtils.setProfileSwitcher(it, navController, mainNotifierViewModel)
        }

        navController.addOnDestinationChangedListener(this)

        mainNotifierViewModel.fabData.observe(this) {
            if (it == null) {
                if (UiUtils.isTabletUi) {
                    (mainFab as ExtendedFloatingActionButton).hide()
                    binding.sidebarNav.updateLayoutParams<MarginLayoutParams> {
                        topMargin = 0
                    }
                } else {
                    (mainFab as FloatingActionButton).hide()
                }

                return@observe
            }

            it.lifecycleOwner.lifecycle.addObserver(
                object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        when (event) {
                            Lifecycle.Event.ON_DESTROY -> {
                                it.lifecycleOwner.lifecycle.removeObserver(this)
                                mainNotifierViewModel.fabData.value = null
                                if (UiUtils.isTabletUi)
                                    binding.sidebarNav.fitsSystemWindows = true
                            }

                            else -> {}
                        }
                    }

                }
            )

            if (UiUtils.isTabletUi) {
                binding.sidebarNav.fitsSystemWindows = false
                (mainFab as ExtendedFloatingActionButton).apply {
                    setIconResource(it.iconRes)
                    setText(it.stringRes)
                    setOnClickListener(it.clickListener)
                    setOnLongClickListener(it.longClickListener)
                    show()
                    binding.sidebarNav.updateLayoutParams<MarginLayoutParams> {
                        topMargin = resources.getDimensionPixelSize(R.dimen.fab_margin)
                    }
                }
            } else {
                (mainFab as FloatingActionButton).apply {
                    setImageResource(it.iconRes)
                    contentDescription = getString(it.stringRes)
                    setOnClickListener(it.clickListener)
                    setOnLongClickListener(it.longClickListener)
                    show()
                }
            }
        }

        billingViewModel.proStatus.observe(this) {
            if (it == true) {
                binding.sidebarNav.menu.removeItem(R.id.nav_pro)
            }
        }
        billingViewModel.queryPurchases()

        if (canShowNotices) {
            lifecycleScope.launch {
                showSnackbarIfNeeded()
            }
        }

        mainNotifierViewModel.drawerData.observe(this) {
            NavUtils.updateHeaderWithDrawerData(
                navHeaderbinding ?: return@observe,
                mainNotifierViewModel
            )
        }
//        showSnackbarIfNeeded()
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

        mainNotifierViewModel.prevDestinationId = destination.id
    }

    private suspend fun showSnackbarIfNeeded() {
        delay(1500)
        val nlsEnabled = Stuff.isNotificationListenerEnabled()

        if (nlsEnabled && !Stuff.isScrobblerRunning()) {
            Snackbar.make(
                binding.root,
                R.string.not_running,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.not_running_fix_action) {
                    navController.navigate(R.id.fixItFragment)
                }
                .focusOnTv()
                .show()
            Timber.tag(Stuff.TAG).w(Exception("${Stuff.SCROBBLER_PROCESS_NAME} not running"))
        } else if (!nlsEnabled || !prefs.scrobblerEnabled) {
            Snackbar.make(
                binding.root,
                R.string.scrobbler_off,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.enable) {
                    if (!prefs.scrobblerEnabled)
                        prefs.scrobblerEnabled = true
                    else
                        navController.navigate(R.id.onboardingFragment)
                }
                .focusOnTv()
                .show()
        } else
            Updater(this, prefs).withSnackbar()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navController.handleDeepLink(intent)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase ?: return)
        setLocaleCompat()
    }

//    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
//        Stuff.log("focus: $currentFocus")
//        return super.onKeyUp(keyCode, event)
//    }

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
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

//    override fun onSaveInstanceState(outState: Bundle) {
//        if (binding.drawerLayout.getDrawerLockMode(GravityCompat.START) == DrawerLayout.LOCK_MODE_LOCKED_OPEN)
//            binding.drawerLayout.isSaveEnabled = false
//        outState.putInt("tab_bar_visible", binding.tabBar.visibility)
//        super.onSaveInstanceState(outState)
//    }
}
