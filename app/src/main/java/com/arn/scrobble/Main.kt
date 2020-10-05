package com.arn.scrobble

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.app.Notification
import android.app.Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES
import android.app.NotificationManager
import android.app.PendingIntent
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
import com.arn.scrobble.charts.ChartsPagerFragment
import com.arn.scrobble.pending.PendingScrService
import com.arn.scrobble.pending.db.PendingScrobblesDb
import com.arn.scrobble.pref.AppListFragment
import com.arn.scrobble.pref.MultiPreferences
import com.arn.scrobble.pref.PrefFragment
import com.arn.scrobble.ui.ShadowDrawerArrowDrawable
import com.arn.scrobble.ui.StatefulAppBar
import com.google.android.material.internal.NavigationMenuItemView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.coordinator_main.*
import kotlinx.android.synthetic.main.coordinator_main.view.*
import kotlinx.android.synthetic.main.header_nav.*
import org.codechimp.apprater.AppRater
import java.io.File

class Main : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        FragmentManager.OnBackStackChangedListener{

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var pref: MultiPreferences
    private lateinit var actPref: SharedPreferences
    private var lastDrawerOpenTime:Long = 0
    private var backArrowShown = false
    var coordinatorPadding = 0
    private var drawerInited = false
    var pendingSubmitAttempted = false
    private lateinit var connectivityCb: ConnectivityManager.NetworkCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        Stuff.timeIt("onCreate start")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        Stuff.timeIt("onCreate setContentView")
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        pref = MultiPreferences(applicationContext)
        actPref = getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
        coordinatorPadding = coordinator.paddingStart
        isTV = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        app_bar.onStateChangeListener = { state ->

            when (state) {
                StatefulAppBar.EXPANDED -> {
                    toolbar.title = null
                    tab_bar.visibility = View.GONE
                }
                StatefulAppBar.IDLE -> {
                    tab_bar.visibility = View.GONE
                }
                StatefulAppBar.COLLAPSED -> {
                    if (supportFragmentManager.findFragmentByTag(Stuff.TAG_HOME_PAGER)?.isVisible == true &&
                            supportFragmentManager.findFragmentByTag(Stuff.TAG_SIMILAR) == null ||
                    supportFragmentManager.findFragmentByTag(Stuff.TAG_CHART_PAGER)?.isVisible == true) {
                        tab_bar.visibility = View.VISIBLE
                    } else {
                        tab_bar.visibility = View.GONE
                    }
                }
            }
        }

        toggle = object: ActionBarDrawerToggle(
                this, drawer_layout, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            override fun onDrawerOpened(drawerView: View) {
                this@Main.onDrawerOpened()
            }
        }
        toggle.drawerArrowDrawable = ShadowDrawerArrowDrawable(drawerToggleDelegate?.actionBarThemedContext)

        if (isTV) {
            hero_calendar.visibility = View.INVISIBLE
            hero_similar.visibility = View.INVISIBLE
            hero_share.visibility = View.INVISIBLE
            hero_info.visibility = View.INVISIBLE
            hero_play.visibility = View.INVISIBLE

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                for (i in 0..ctl.toolbar.childCount) {
                    val child = ctl.toolbar.getChildAt(i)
                    if (child is ImageButton) {
                        child.setFocusable(false)
                        break
                    }
                }
        }
        drawer_layout.addDrawerListener(toggle)
        nav_view.setNavigationItemSelectedListener(this)

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
                else if (directOpenExtra == Stuff.DL_CHARTS)
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.frame, ChartsPagerFragment(), Stuff.TAG_CHART_PAGER)
                            .addToBackStack(null)
                            .commit()
                else {
                    if (coordinatorPadding > 0)
                        drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED) //for some devices
                    showHomePager()

                    val handler = Handler(mainLooper)
                    handler.post {
                        if (!KeepNLSAliveJob.ensureServiceRunning(this))
                            showNotRunning()
                        else if (!isTV)
                            AppRater.app_launched(this)
                    }
                }
            } else {
                showFirstThings(hidePassBox)
            }
        } else {
            tab_bar.visibility = savedInstanceState.getInt("tab_bar_visible", View.GONE)
            if (supportFragmentManager.findFragmentByTag(Stuff.TAG_HOME_PAGER)?.isAdded == true &&
                    supportFragmentManager.backStackEntryCount == 0)
                openLockDrawer()
        }
        supportFragmentManager.addOnBackStackChangedListener(this)
//        showNotRunning()
//        test()
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
        app_bar.setExpanded(false, true)
        closeLockDrawer()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
        val lockMode = drawer_layout.getDrawerLockMode(GravityCompat.START)
        backArrowShown = lockMode == DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        toggle.onDrawerSlide(drawer_layout, if (backArrowShown) 1f else 0f)

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
                PendingIntent.FLAG_UPDATE_CURRENT)

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
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorNoti))
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

    private fun onDrawerOpened(forceUpdate: Boolean = false){
        if (drawer_layout?.isDrawerVisible(GravityCompat.START) != true || (!forceUpdate &&
                        System.currentTimeMillis() - lastDrawerOpenTime < Stuff.RECENTS_REFRESH_INTERVAL))
            return

        val username = pref.getString(Stuff.PREF_LASTFM_USERNAME,"nobody")
        nav_name.text = if (BuildConfig.DEBUG) "nobody" else username
        val numToday = actPref.getInt(Stuff.PREF_ACTIVITY_TODAY_SCROBBLES, 0)
        val numTotal = actPref.getInt(Stuff.PREF_ACTIVITY_TOTAL_SCROBBLES, 0)
        nav_num_scrobbles.text = getString(R.string.num_scrobbles_nav, numTotal, numToday)

        nav_profile_link.setOnClickListener { v:View ->
            Stuff.openInBrowser("https://www.last.fm/user/$username", this, v)
        }
        val picUrl = actPref.getString(Stuff.PREF_ACTIVITY_PROFILE_PIC,"")
        if (picUrl != "")
            Picasso.get()
                    .load(picUrl)
                    .noPlaceholder()
                    .error(R.drawable.vd_wave)
                    .into(nav_profile_pic)
        if (!forceUpdate)
            LFMRequester(applicationContext).getDrawerInfo().asAsyncTask()
        lastDrawerOpenTime = System.currentTimeMillis()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (coordinatorPadding == 0)
            drawer_layout.closeDrawer(GravityCompat.START)

        when (item.itemId) {
            R.id.nav_last_week -> {
                val username = pref.getString(Stuff.PREF_LASTFM_USERNAME,"nobody")
                Stuff.openInBrowser("https://www.last.fm/user/$username/listening-report/week", this, frame, 10, 200)
            }
            R.id.nav_recents -> {
                tab_bar.getTabAt(0)?.select()
            }
            R.id.nav_loved -> {
                tab_bar.getTabAt(1)?.select()
            }
            R.id.nav_friends -> {
                tab_bar.getTabAt(2)?.select()
            }
            R.id.nav_charts -> {
                enableGestures()
                supportFragmentManager.beginTransaction()
                        .replace(R.id.frame, ChartsPagerFragment(), Stuff.TAG_CHART_PAGER)
                        .addToBackStack(null)
                        .commit()
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
        }
        return true
    }

    private fun enableGestures() {
        val hp = supportFragmentManager.findFragmentByTag(Stuff.TAG_HOME_PAGER) as? HomePagerFragment
        hp?.setGestureExclusions(false)
    }

    override fun onBackStackChanged() {
        if (app_bar != null) {
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

            val expand = pager != null && pager.currentItem != 2 &&
                    supportFragmentManager.findFragmentByTag(Stuff.TAG_FIRST_THINGS)?.isVisible != true ||
                    supportFragmentManager.findFragmentByTag(Stuff.TAG_SIMILAR)?.isVisible == true

            app_bar.setExpanded(expand, animate)
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START) && coordinatorPadding == 0)
            drawer_layout.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }

    private fun showNotRunning(){
        val snackbar = Snackbar
                .make(frame, R.string.not_running, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.not_running_fix_action) {
                    FixItFragment().show(supportFragmentManager, null)
                }
                .setActionTextColor(Color.YELLOW)
        snackbar.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onShown(sb: Snackbar?) {
                super.onShown(sb)
                if (sb != null && isTV)
                    sb.view.postDelayed({
                        sb.view.findViewById<View>(com.google.android.material.R.id.snackbar_action)
                                .requestFocus()
                }, 200)
            }
        })
        snackbar.show()
    }

    private fun mailLogs(){
        val activeSessions = try {
            val sessManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            sessManager.getActiveSessions(ComponentName(this, NLService::class.java))
                    .fold("") { str, session -> str +  ", "  + session.packageName}
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

        text += "------------------------\n\n[how did this happen?]\n"
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
            drawer_layout.openDrawer(GravityCompat.START)
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
        }
    }

    private fun showBackArrow(show: Boolean){
        if (backArrowShown != show) {
            val start = if (show) 0f else 1f
            val anim = ValueAnimator.ofFloat(start, 1 - start)
            anim.addUpdateListener { valueAnimator ->
                val slideOffset = valueAnimator.animatedValue as Float
                toggle.onDrawerSlide(drawer_layout, slideOffset)
            }
            anim.interpolator = DecelerateInterpolator()
            anim.startDelay = 200
            anim.duration = 1000
            anim.start()

            when {
                show -> closeLockDrawer()
                coordinatorPadding > 0 -> openLockDrawer()
                else -> drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }

            backArrowShown = show
        }
    }

    private val mainReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                NLService.iDRAWER_UPDATE -> onDrawerOpened(true)
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        val iF = IntentFilter()
        iF.addAction(NLService.iDRAWER_UPDATE)
        registerReceiver(mainReceiver, iF)

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
        drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        if (coordinatorPadding > 0)
            coordinator.setPadding(0,0,0,0)
    }


    private fun openLockDrawer(){
        if(coordinatorPadding > 0) {
            drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
            if (!drawerInited) {
                nav_view.addOnLayoutChangeListener { view, left, top, right, bottom,
                                                     leftWas, topWas, rightWas, bottomWas ->
                    if (left != leftWas || right != rightWas)
                        onDrawerOpened()
                }
                drawerInited = true
            }
            if (coordinator.paddingStart != coordinatorPadding)
                coordinator.setPaddingRelative(coordinatorPadding,0,0,0)
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
        unregisterReceiver(mainReceiver)
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(connectivityCb)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (drawer_layout.getDrawerLockMode(GravityCompat.START) == DrawerLayout.LOCK_MODE_LOCKED_OPEN)
            drawer_layout.isSaveEnabled = false
        outState.putInt("tab_bar_visible", tab_bar.visibility)
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
