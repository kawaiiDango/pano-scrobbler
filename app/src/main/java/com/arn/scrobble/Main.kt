package com.arn.scrobble

import android.animation.ValueAnimator
import android.app.*
import android.app.Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES
import android.content.*
import android.content.pm.LabeledIntent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.media.session.MediaSessionManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.media.app.MediaStyleMod
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager
import com.arn.scrobble.billing.BillingFragment
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.databinding.ActivityMainBinding
import com.arn.scrobble.databinding.HeaderNavBinding
import com.arn.scrobble.pending.PendingScrService
import com.arn.scrobble.db.PendingScrobblesDb
import com.arn.scrobble.info.InfoFragment
import com.arn.scrobble.pref.AppListFragment
import com.arn.scrobble.pref.MultiPreferences
import com.arn.scrobble.pref.PrefFragment
import com.arn.scrobble.search.SearchFragment
import com.arn.scrobble.themes.ColorPatchUtils
import com.arn.scrobble.ui.ShadowDrawerArrowDrawable
import com.arn.scrobble.ui.StatefulAppBar
import com.google.android.material.color.MaterialColors
import com.google.android.material.internal.NavigationMenuItemView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import org.codechimp.apprater.AppRater
import timber.log.Timber
import java.io.File
import java.text.NumberFormat
import java.util.*

class Main : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        FragmentManager.OnBackStackChangedListener{

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var pref: MultiPreferences
    private var lastDrawerOpenTime:Long = 0
    private var backArrowShown = false
    var coordinatorPadding = 0
    private var drawerInited = false
    var pendingSubmitAttempted = false
    lateinit var binding: ActivityMainBinding
    private lateinit var navHeaderbinding: HeaderNavBinding
    private lateinit var connectivityCb: ConnectivityManager.NetworkCallback
    val billingViewModel by lazy { VMFactory.getVM(this, BillingViewModel::class.java) }
    val mainNotifierViewModel by lazy { VMFactory.getVM(this, MainNotifierViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        Stuff.timeIt("onCreate start")
        super.onCreate(savedInstanceState)

        if (billingViewModel.proStatus.value == true)
            ColorPatchUtils.setTheme(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        navHeaderbinding = HeaderNavBinding.inflate(layoutInflater, binding.navView, false)
        binding.navView.addHeaderView(navHeaderbinding.root)
        binding.drawerLayout.drawerElevation = 0f
        setContentView(binding.root)
        Stuff.timeIt("onCreate setContentView")
        setSupportActionBar(binding.coordinatorMain.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        pref = MultiPreferences(applicationContext)
        coordinatorPadding = binding.coordinatorMain.coordinator.paddingStart
        isTV = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        binding.coordinatorMain.appBar.onStateChangeListener = { state ->

            when (state) {
                StatefulAppBar.EXPANDED -> {
                    binding.coordinatorMain.toolbar.title = null
                    binding.coordinatorMain.tabBar.visibility = View.GONE
                }
                StatefulAppBar.IDLE -> {
                    binding.coordinatorMain.tabBar.visibility = View.GONE
                }
                StatefulAppBar.COLLAPSED -> {
                    if (supportFragmentManager.findFragmentByTag(Stuff.TAG_HOME_PAGER)?.isVisible == true ||
                    supportFragmentManager.findFragmentByTag(Stuff.TAG_CHART_PAGER)?.isVisible == true) {
                        binding.coordinatorMain.tabBar.visibility = View.VISIBLE
                    } else {
                        binding.coordinatorMain.tabBar.visibility = View.GONE
                    }
                }
            }
        }

        toggle = object: ActionBarDrawerToggle(
                this, binding.drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            override fun onDrawerOpened(drawerView: View) {
                mainNotifierViewModel.drawerData.value?.let {
                    this@Main.onDrawerOpened()
                }
            }
        }
        toggle.drawerArrowDrawable = ShadowDrawerArrowDrawable(drawerToggleDelegate?.actionBarThemedContext)

        if (isTV) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                for (i in 0..binding.coordinatorMain.toolbar.childCount) {
                    val child = binding.coordinatorMain.toolbar.getChildAt(i)
                    if (child is ImageButton) {
                        child.setFocusable(false)
                        break
                    }
                }
        }
        binding.drawerLayout.addDrawerListener(toggle)
        binding.navView.setNavigationItemSelectedListener(this)

        val hidePassBox =
            if (intent.data?.isHierarchical == true && intent.data?.path == "/testFirstThings"){
                pref.remove(Stuff.PREF_LASTFM_SESS_KEY)
                true
            } else
                false

        if (savedInstanceState == null) {
            if (FirstThingsFragment.checkAuthTokenExists(pref) &&
                FirstThingsFragment.checkNLAccess(this)) {

                val directOpenExtra = intent?.getIntExtra(Stuff.DIRECT_OPEN_KEY, 0) ?: 0

                if (directOpenExtra == Stuff.DL_SETTINGS || intent?.categories?.contains(INTENT_CATEGORY_NOTIFICATION_PREFERENCES) == true)
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.frame, PrefFragment())
                            .addToBackStack(null)
                            .commit()
                else if (directOpenExtra == Stuff.DL_APP_LIST)
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.frame, AppListFragment())
                            .addToBackStack(null)
                            .commit()
                else if (directOpenExtra == Stuff.DL_MIC)
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.frame, RecFragment())
                            .addToBackStack(null)
                            .commit()
                else if (directOpenExtra == Stuff.DL_SEARCH)
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.frame, SearchFragment())
                            .addToBackStack(null)
                            .commit()
                else {
                    if (coordinatorPadding > 0)
                        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED) //for some devices
                    showHomePager()

                    if (intent.getStringExtra(NLService.B_ARTIST) != null)
                            showInfoFragment(intent)
                    else {
                        val handler = Handler(mainLooper)
                        handler.post {
                            if (!KeepNLSAliveJob.ensureServiceRunning(this))
                                showNotRunning()
                            else if (!isTV && billingViewModel.proStatus.value != true)
                                AppRater.app_launched(this)
                        }
                    }
                }
            } else {
                showFirstThings(hidePassBox)
            }
        } else {
            binding.coordinatorMain.tabBar.visibility = savedInstanceState.getInt("tab_bar_visible", View.GONE)
            if (supportFragmentManager.findFragmentByTag(Stuff.TAG_HOME_PAGER)?.isAdded == true &&
                    supportFragmentManager.backStackEntryCount == 0)
                openLockDrawer()
        }
        supportFragmentManager.addOnBackStackChangedListener(this)
        billingViewModel.proStatus.observe(this) {
            if (it == true) {
                binding.navView.menu.removeItem(R.id.nav_pro)
            }
        }
        billingViewModel.queryPurchases()
        mainNotifierViewModel.drawerData.observe(this) {
            it?.let { drawerData ->

                val nf = NumberFormat.getInstance()
                navHeaderbinding.navNumScrobbles.text = getString(R.string.num_scrobbles_nav,
                    nf.format(drawerData.totalScrobbles), nf.format(drawerData.todayScrobbles))

                if (drawerData.profilePicUrl != "")
                    Picasso.get()
                        .load(drawerData.profilePicUrl)
                        .noPlaceholder()
                        .error(R.drawable.vd_wave)
                        .into(navHeaderbinding.navProfilePic)
                else
                    navHeaderbinding.navProfilePic.setImageResource(R.drawable.vd_wave)
            }
        }
//        showNotRunning()
//        testNoti()
    }

    fun showHomePager(){
        openLockDrawer()
        supportFragmentManager.beginTransaction()
                .replace(R.id.frame, HomePagerFragment(), Stuff.TAG_HOME_PAGER)
                .commit()
    }

    private fun showFirstThings(hidePassBox: Boolean) {
        val b = Bundle()
        b.putBoolean(Stuff.ARG_NOPASS, hidePassBox)
        val f = FirstThingsFragment()
        f.arguments = b
        supportFragmentManager.beginTransaction()
                .replace(R.id.frame, f, Stuff.TAG_FIRST_THINGS)
                .commit()
        binding.coordinatorMain.appBar.setExpanded(false, true)
        closeLockDrawer()
    }

    private fun showInfoFragment(intent: Intent){
        val artist = intent.getStringExtra(NLService.B_ARTIST)
        val album = intent.getStringExtra(NLService.B_ALBUM)
        val track = intent.getStringExtra(NLService.B_TITLE)
        val info = InfoFragment()
        info.arguments = Bundle().apply {
            putString(NLService.B_ARTIST, artist)
            putString(NLService.B_ALBUM, album)
            putString(NLService.B_TITLE, track)
        }
        supportFragmentManager.findFragmentByTag(Stuff.TAG_INFO_FROM_WIDGET)?.let {
            (it as InfoFragment).dismiss()
        }
        info.show(supportFragmentManager, Stuff.TAG_INFO_FROM_WIDGET)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
        val lockMode = binding.drawerLayout.getDrawerLockMode(GravityCompat.START)
        backArrowShown = lockMode == DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        toggle.onDrawerSlide(binding.drawerLayout, if (backArrowShown) 1f else 0f)

        Stuff.timeIt("onPostCreate")
    }

    fun testNoti (){
        AppRater.showRateSnackbar(this)
        val res = Resources.getSystem()
        val attrs = arrayOf(android.R.attr.textColor).toIntArray()

        var sysStyle = res.getIdentifier("TextAppearance.Material.Notification.Title", "style", "android")
        val titleTextColor = obtainStyledAttributes(sysStyle, attrs).getColor(0, Color.BLACK)

        sysStyle = res.getIdentifier("TextAppearance.Material.Notification", "style", "android")
        val secondaryTextColor = obtainStyledAttributes(sysStyle, attrs).getColor(0, Color.BLACK)

        Stuff.log("clr: $titleTextColor $secondaryTextColor")

        val longDescription = SpannableStringBuilder()
        longDescription.append("def ")

        var start = longDescription.length
        longDescription.append("c1 ")
        longDescription.setSpan(ForegroundColorSpan(ContextCompat.getColor(applicationContext, android.R.color.secondary_text_light)), start, longDescription.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        start = longDescription.length
        longDescription.append("c2 ")
        longDescription.setSpan(ForegroundColorSpan(titleTextColor), start, longDescription.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        start = longDescription.length
        longDescription.append("c3 ")
        longDescription.setSpan(ForegroundColorSpan(secondaryTextColor), start, longDescription.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//        longDescription.setSpan(StyleSpan(android.graphics.Typeface.BOLD), start, longDescription.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        longDescription.append(" rest")

        val launchIntent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, Main::class.java)
                .putExtra(Stuff.DIRECT_OPEN_KEY, Stuff.DL_APP_LIST),
                Stuff.updateCurrentOrImmutable)

        val style = MediaStyleMod()//android.support.v4.media.app.NotificationCompat.MediaStyle()
        style.setShowActionsInCompactView(0, 1)
        val icon = ContextCompat.getDrawable(this, R.drawable.ic_launcher)
//        icon.setColorFilter(ContextCompat.getColor(applicationContext, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)

        val nb = NotificationCompat.Builder(applicationContext, NLService.NOTI_ID_SCR)
                .setSmallIcon(R.drawable.vd_noti)
//                .setLargeIcon(Stuff.drawableToBitmap(icon))
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setAutoCancel(true)
                .setShowWhen(false)
                .setUsesChronometer(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .addAction(R.drawable.vd_undo, getString(R.string.unscrobble), launchIntent)
                .addAction(R.drawable.vd_check, getString(R.string.unscrobble), launchIntent)
                .setContentTitle("setContentTitle")
                .setContentText("longDescription")
                .setSubText("setSubText")
                .setColor(MaterialColors.getColor(this, R.attr.colorNoti, null))
                .setStyle(style)
//                .setCustomBigContentView(null)
//                .setCustomContentView(null)

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = nb.build()
        n.bigContentView = null
        val rv = n.contentView
/*
        var resId = res.getIdentifier("title", "id", "android")
        rv.setTextColor(resId, Color.BLACK)
        resId = res.getIdentifier("text", "id", "android")
        rv.setTextColor(resId, Color.BLACK)
        resId = res.getIdentifier("text2", "id", "android")
        rv.setTextColor(resId, Color.BLACK)
        resId = res.getIdentifier("status_bar_latest_event_content", "id", "android")
        Stuff.log("resId $resId")
        rv.setInt(resId, "setBackgroundColor", R.drawable.notification_bg)

        resId = res.getIdentifier("action0", "id", "android")
        val c = Class.forName("android.widget.RemoteViews")
        val m = c.getMethod("setDrawableParameters", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, PorterDuff.Mode::class.java, Int::class.javaPrimitiveType)
        m.invoke(rv, resId, false, -1, ContextCompat.getColor(applicationContext, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_ATOP, -1)
        rv.setImageViewResource(resId, R.drawable.vd_ban)
*/
        nm.notify(9, n)

    }

    private fun onDrawerOpened(){
        if (!binding.drawerLayout.isDrawerVisible(GravityCompat.START) || (
                        System.currentTimeMillis() - lastDrawerOpenTime < Stuff.RECENTS_REFRESH_INTERVAL))
            return

        LFMRequester(applicationContext).getDrawerInfo().asAsyncTask(mainNotifierViewModel.drawerData)

        val username = pref.getString(Stuff.PREF_LASTFM_USERNAME,"nobody")
        val displayUsername = if (BuildConfig.DEBUG) "nobody" else username
        if (navHeaderbinding.navName.tag == null)
            navHeaderbinding.navName.text = displayUsername

        navHeaderbinding.navProfileLink.setOnClickListener {
            Stuff.openInBrowser("https://www.last.fm/user/$username", this)
        }

        lastDrawerOpenTime = System.currentTimeMillis()

        if (navHeaderbinding.navName.tag == null) {
            val cal = Calendar.getInstance()
            val c = (cal[Calendar.MONTH] == 11 && cal[Calendar.DAY_OF_MONTH] >= 25) ||
                    (cal[Calendar.MONTH] == 0 && cal[Calendar.DAY_OF_MONTH] <= 5)
            if (!c)
                return
            navHeaderbinding.navName.tag = "☃️"
            val runnable = object : Runnable {
                override fun run() {
                    if (navHeaderbinding.navName.tag == "☃️")
                        navHeaderbinding.navName.tag = "⛄️"
                    else
                        navHeaderbinding.navName.tag = "☃️"
                    navHeaderbinding.navName.text = (navHeaderbinding.navName.tag as String) + displayUsername + "\uD83C\uDF84"
                    navHeaderbinding.navName.postDelayed(this, 500)
                }
            }
            runnable.run()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (coordinatorPadding == 0)
            binding.drawerLayout.closeDrawer(GravityCompat.START)

        when (item.itemId) {
            R.id.nav_last_week -> {
                val username = pref.getString(Stuff.PREF_LASTFM_USERNAME,"nobody")
                Stuff.openInBrowser("https://www.last.fm/user/$username/listening-report/week", this, binding.coordinatorMain.frame, 10, 200)
            }
            R.id.nav_recents -> {
                binding.coordinatorMain.tabBar.getTabAt(0)?.select()
            }
            R.id.nav_loved -> {
                binding.coordinatorMain.tabBar.getTabAt(1)?.select()
            }
            R.id.nav_friends -> {
                binding.coordinatorMain.tabBar.getTabAt(2)?.select()
            }
            R.id.nav_charts -> {
                binding.coordinatorMain.tabBar.getTabAt(3)?.select()
            }
            R.id.nav_random -> {
                enableGestures()
                supportFragmentManager.beginTransaction()
                        .replace(R.id.frame, RandomFragment())
                        .addToBackStack(null)
                        .commit()
            }
            R.id.nav_rec -> {
                enableGestures()
                supportFragmentManager.beginTransaction()
                        .replace(R.id.frame, RecFragment())
                        .addToBackStack(null)
                        .commit()
            }
            R.id.nav_search -> {
                enableGestures()
                supportFragmentManager.beginTransaction()
                        .replace(R.id.frame, SearchFragment())
                        .addToBackStack(null)
                        .commit()
            }
            R.id.nav_settings -> {
                enableGestures()
                supportFragmentManager.beginTransaction()
                        .replace(R.id.frame, PrefFragment())
                        .addToBackStack(null)
                        .commit()
            }
            R.id.nav_report -> {
                mailLogs()
            }
            R.id.nav_pro -> {
                enableGestures()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.frame, BillingFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
        return true
    }

    fun enableGestures() {
        val hp = supportFragmentManager.findFragmentByTag(Stuff.TAG_HOME_PAGER) as? HomePagerFragment
        hp?.setGestureExclusions(false)
    }

    override fun onBackStackChanged() {
//        if (app_bar != null) {
            val animate = true
            if (supportFragmentManager.backStackEntryCount == 0) {
                val firstThingsVisible = supportFragmentManager.findFragmentByTag(Stuff.TAG_FIRST_THINGS)?.isVisible
                // what the fuck, kotlin extensions? stop giving me old instances

                if (firstThingsVisible != true)
                    showBackArrow(false)

                if (supportFragmentManager.fragments.isEmpty()) //came back from direct open
                    showHomePager()
            } else {
                showBackArrow(true)
            }

            val pager = supportFragmentManager.findFragmentByTag(Stuff.TAG_HOME_PAGER)?.view?.findViewById<ViewPager>(R.id.pager)

            val expand = pager != null && pager.currentItem != 2 && pager.currentItem != 3 &&
                    supportFragmentManager.findFragmentByTag(Stuff.TAG_FIRST_THINGS)?.isVisible != true

            binding.coordinatorMain.appBar.setExpanded(expand, animate)
//        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START) && coordinatorPadding == 0)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }

    private fun showNotRunning(){
        Snackbar
                .make(binding.coordinatorMain.frame, R.string.not_running, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.not_running_fix_action) {
                    FixItFragment().show(supportFragmentManager, null)
                }
                .addCallback(object : Snackbar.Callback() {
                    override fun onShown(sb: Snackbar?) {
                        super.onShown(sb)
                        if (sb != null && isTV)
                            sb.view.postDelayed({
                                sb.view.findViewById<View>(com.google.android.material.R.id.snackbar_action)
                                        .requestFocus()
                        }, 200)
                    }
            })
            .show()
        Timber.tag(Stuff.TAG).w(Exception("bgScrobbler not running"))
    }

    private fun mailLogs(){
        val activeSessions = try {
            val sessManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            sessManager.getActiveSessions(ComponentName(this, NLService::class.java))
                .joinToString { it.packageName }
        } catch (e: SecurityException) {
            "SecurityException"
        }
        var bgRam = -1
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (proc in manager.runningAppProcesses){
            if (proc?.processName?.contains("bgScrobbler") == true){
                // https://stackoverflow.com/questions/2298208/how-do-i-discover-memory-usage-of-my-application-in-android
                val memInfo = manager.getProcessMemoryInfo(intArrayOf(proc.pid)).first()
                bgRam = memInfo.totalPss / 1024
                break
            }
        }


        var text = ""
        text += getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME+ "\n"
        text += "Android " + Build.VERSION.RELEASE+ "\n"
        text += "ROM: " + Build.DISPLAY+ "\n"
        text += "Device: " + Build.BRAND + " " + Build.MODEL + " / " + Build.DEVICE + "\n" //Build.PRODUCT is obsolete

        val mi = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(mi)
        val megs = mi.totalMem / 1048576L
        text += "RAM: " + megs + "M \n"
        text += "Background RAM usage: " + bgRam + "M \n"

        val dm = resources.displayMetrics

        text += "Screen: " + dm.widthPixels + " x " + dm.heightPixels + ",  " + dm.densityDpi + " DPI\n"

        if (!KeepNLSAliveJob.ensureServiceRunning(this))
            text += "Background service isn't running\n"
        text += "Active Sessions: $activeSessions\n"

        text += if (billingViewModel.proStatus.value == true)
            "~~~~~~~~~~~~~~~~~~~~~~~~"
        else
            "------------------------"
        text += "\n\n[how did this happen?]\n"
        //keep the email in english

        val log = Stuff.exec("logcat -d")
        val logFile = File(filesDir, "log.txt")
        logFile.writeText(log)
        val logUri = FileProvider.getUriForFile(this, "com.arn.scrobble.fileprovider", logFile)

//        PendingScrobblesDb.destroyInstance()
//        val dbFile = File(filesDir, PendingScrobblesDb.tableName + ".sqlite")
//        getDatabasePath(PendingScrobblesDb.tableName).copyTo(dbFile, true)
//        val dbUri = FileProvider.getUriForFile(this, "com.arn.scrobble.fileprovider", dbFile)

        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "huh@huh.com", null))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "huh?")
        val resolveInfos = packageManager.queryIntentActivities(emailIntent, 0)
        val intents = arrayListOf<LabeledIntent>()
        for (info in resolveInfos) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.email)))
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) +" - Bug report")
            intent.putExtra(Intent.EXTRA_TEXT, text)
            intent.putExtra(Intent.EXTRA_STREAM, logUri)
            intents.add(LabeledIntent(intent, info.activityInfo.packageName, info.loadLabel(packageManager), info.icon))
        }
        if (intents.size > 0) {
            val chooser = Intent.createChooser(intents.removeAt(intents.size - 1), getString(R.string.action_report))
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
            startActivity(chooser)
        }else
            Stuff.toast(this, getString(R.string.no_mail_apps))
    }

    override fun onSupportNavigateUp(): Boolean {
        if (backArrowShown)
            supportFragmentManager.popBackStack()
        else
            binding.drawerLayout.openDrawer(GravityCompat.START)
        return true
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.data?.isHierarchical == true) {
            val uri = intent.data!!
            val path = uri.path
            val token = uri.getQueryParameter("token")
            if (token != null){
                Stuff.log("onNewIntent got token for $path")
                when(path) {
                    "/lastfm" ->
                        LFMRequester(applicationContext).doAuth(R.string.lastfm, token).asAsyncTask()
                    "/librefm" ->
                        LFMRequester(applicationContext).doAuth(R.string.librefm, token).asAsyncTask()
                    "/gnufm" ->
                        LFMRequester(applicationContext).doAuth(R.string.gnufm, token).asAsyncTask()
                    "/testFirstThings" -> {
                        pref.remove(Stuff.PREF_LASTFM_SESS_KEY)
                        for (i in 0..supportFragmentManager.backStackEntryCount)
                            supportFragmentManager.popBackStackImmediate()
                        showFirstThings(true)
                    }
                }
            }
        } else if (intent?.getStringExtra(NLService.B_ARTIST) != null)
            showInfoFragment(intent)
    }

    private fun showBackArrow(show: Boolean){
        if (backArrowShown != show) {
            val start = if (show) 0f else 1f
            val anim = ValueAnimator.ofFloat(start, 1 - start)
            anim.addUpdateListener { valueAnimator ->
                val slideOffset = valueAnimator.animatedValue as Float
                toggle.onDrawerSlide(binding.drawerLayout, slideOffset)
            }
            anim.interpolator = DecelerateInterpolator()
            anim.startDelay = 200
            anim.duration = 1000
            anim.start()

            when {
                show -> closeLockDrawer()
                coordinatorPadding > 0 -> openLockDrawer()
                else -> binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }

            backArrowShown = show
        }
    }

    public override fun onStart() {
        super.onStart()

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder = NetworkRequest.Builder()
        connectivityCb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
            }

            override fun onLost(network: Network) {
                isOnline = cm.activeNetworkInfo?.isConnected == true
            }

            override fun onUnavailable() {
                isOnline = cm.activeNetworkInfo?.isConnected == true
            }
        }

        cm.registerNetworkCallback(builder.build(), connectivityCb)

        val ni = cm.activeNetworkInfo
        isOnline = ni?.isConnected == true
    }

    private fun closeLockDrawer(){
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        if (coordinatorPadding > 0)
            binding.coordinatorMain.coordinator.setPadding(0,0,0,0)
    }


    private fun openLockDrawer(){
        if(coordinatorPadding > 0) {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
            if (!drawerInited) {
                binding.navView.addOnLayoutChangeListener { view, left, top, right, bottom,
                                                     leftWas, topWas, rightWas, bottomWas ->
                    if (left != leftWas || right != rightWas)
                        onDrawerOpened()
                }
                drawerInited = true
            }
            if (binding.coordinatorMain.coordinator.paddingStart != coordinatorPadding)
                binding.coordinatorMain.coordinator.setPaddingRelative(coordinatorPadding,0,0,0)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        Stuff.log("focus: $currentFocus")
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            val f = currentFocus
            if (f is NavigationMenuItemView) {
                if (resources.getBoolean(R.bool.is_rtl))
                    f.nextFocusLeftId = R.id.pager
                else
                    f.nextFocusRightId = R.id.pager
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    public override fun onStop() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(connectivityCb)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (binding.drawerLayout.getDrawerLockMode(GravityCompat.START) == DrawerLayout.LOCK_MODE_LOCKED_OPEN)
            binding.drawerLayout.isSaveEnabled = false
        outState.putInt("tab_bar_visible", binding.coordinatorMain.tabBar.visibility)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        MultiPreferences.destroyClient()
        if (!PendingScrService.mightBeRunning)
            PendingScrobblesDb.destroyInstance()
        super.onDestroy()
    }

    companion object {
        var isOnline = true
        var isTV = false
    }
}
