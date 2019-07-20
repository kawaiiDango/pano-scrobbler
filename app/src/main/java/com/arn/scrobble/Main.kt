package com.arn.scrobble

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.app.Notification
import android.app.Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.LabeledIntent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
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
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
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
import com.arn.scrobble.pending.PendingScrService
import com.arn.scrobble.pending.db.PendingScrobblesDb
import com.arn.scrobble.pref.AppListFragment
import com.arn.scrobble.pref.MultiPreferences
import com.arn.scrobble.pref.PrefFragment
import com.arn.scrobble.ui.ShadowDrawerArrowDrawable
import com.arn.scrobble.ui.StatefulAppBar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.coordinator_main.*
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
    private var connectivityCb: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Stuff.timeIt("onCreate start")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        Stuff.timeIt("onCreate setContentView")
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        pref = MultiPreferences(applicationContext)
        actPref = getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
//        NLService.migratePrefs(pref)
//        app_bar.onStateChangeListener?.invoke(app_bar.state)
//        tab_bar.visibility = View.GONE
        app_bar.onStateChangeListener = { state ->

            when (state) {
                StatefulAppBar.EXPANDED -> {
                    toolbar.title = " "
                    tab_bar.visibility = View.GONE
                }
                StatefulAppBar.IDLE -> {
                    tab_bar.visibility = View.GONE
                }
                StatefulAppBar.COLLAPSED -> {
                    if (supportFragmentManager.findFragmentByTag(Stuff.TAG_PAGER)?.isVisible == true &&
                            supportFragmentManager.findFragmentByTag(Stuff.TAG_SIMILAR) == null) {
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

        drawer_layout.addDrawerListener(toggle)
        nav_view.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null) {
            if (FirstThingsFragment.checkAuthTokenExists(pref) &&
                FirstThingsFragment.checkNLAccess(this)) {

                val deepLinkExtra = intent?.getIntExtra(Stuff.DEEP_LINK_KEY, 0) ?: 0
                val pagerFragment = PagerFragment()

                supportFragmentManager.beginTransaction()
                        .replace(R.id.frame, pagerFragment, Stuff.TAG_PAGER)//, Stuff.GET_RECENTS)
                        .commit()
//            fragmentManager.beginTransaction()
//                    .hide(recentsFragment)
//                    .add(R.id.frame, FriendsFragment())
//                    .addToBackStack(null)
//                    .commit()
                if (deepLinkExtra == Stuff.DL_SETTINGS || intent?.categories?.contains(INTENT_CATEGORY_NOTIFICATION_PREFERENCES) == true)
                    supportFragmentManager.beginTransaction()
//                            .hide(recentsFragment)
//                            .add(R.id.frame, PrefFragment())
                            .replace(R.id.frame, PrefFragment())
                            .addToBackStack(null)
                            .commit()
                else if (deepLinkExtra == Stuff.DL_APP_LIST)
                    supportFragmentManager.beginTransaction()
//                            .hide(recentsFragment)
//                            .add(R.id.frame, AppListFragment())
                            .replace(R.id.frame, AppListFragment())
                            .addToBackStack(null)
                            .commit()
                else {
                    val handler = Handler()
                    handler.post {
                        if (!KeepNLSAliveJob.ensureServiceRunning(this))
                            handler.postDelayed({
                                if (!KeepNLSAliveJob.ensureServiceRunning(this))
                                    showNotRunning()
                            },2000)
                        else
                            AppRater.app_launched(this)
                    }
                }
            } else {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.frame, FirstThingsFragment())
                        .commit()
                app_bar.setExpanded(false, true)
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
        } else {
            tab_bar.visibility = savedInstanceState.getInt("tab_bar_visible", View.GONE)
        }
        supportFragmentManager.addOnBackStackChangedListener(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawer_layout.systemGestureExclusionRects = listOf(Rect(0, 0, 100, 2500))
            //it doesnt work out of the box
        }
//        showNotRunning()
//        test()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
        val lockMode = drawer_layout.getDrawerLockMode(GravityCompat.START)
        backArrowShown = lockMode == DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        toggle.onDrawerSlide(drawer_layout, if (backArrowShown) 1f else 0f)

        Stuff.timeIt("onPostCreate")
//        test()
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
                .putExtra(Stuff.DEEP_LINK_KEY, Stuff.DL_APP_LIST),
                PendingIntent.FLAG_UPDATE_CURRENT)

        val style = MediaStyleMod()//android.support.v4.media.app.NotificationCompat.MediaStyle()
        style.setShowActionsInCompactView(0, 1)
        val icon = getDrawable(R.mipmap.ic_launcher)
//        icon.setColorFilter(ContextCompat.getColor(applicationContext, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)

        val nb = NotificationCompat.Builder(applicationContext, NLService.NOTI_ID_SCR)
                .setSmallIcon(R.drawable.ic_noti)
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
        val num = actPref.getInt(Stuff.PREF_ACTIVITY_NUM_SCROBBLES, 0)
        nav_num_scrobbles.text = resources.getQuantityString(R.plurals.num_scrobbles, num, num)

        nav_profile_link.setOnClickListener { v:View ->
            Stuff.openInBrowser("https://www.last.fm/user/$username", this, v)
        }
        val picUrl = actPref.getString(Stuff.PREF_ACTIVITY_PROFILE_PIC,"")
        if (picUrl != "")
            Picasso.get()
                    .load(picUrl)
                    .noPlaceholder()
                    .error(R.drawable.vd_wave)
                    .centerCrop()
                    .fit()
                    .into(nav_profile_pic)
        if (!forceUpdate)
            LFMRequester(Stuff.GET_DRAWER_INFO).asAsyncTask(applicationContext)
        lastDrawerOpenTime = System.currentTimeMillis()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val username = pref.getString(Stuff.PREF_LASTFM_USERNAME,"nobody")
        when (item.itemId) {
            R.id.nav_last_week -> {
                Stuff.openInBrowser("https://www.last.fm/user/$username/listening-report/week", this, frame, 10, 200)
            }
            R.id.nav_loved -> {
                tab_bar.getTabAt(1)?.select()
            }
            R.id.nav_friends -> {
                tab_bar.getTabAt(2)?.select()
            }
            R.id.nav_settings -> {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.frame, PrefFragment())
                        .addToBackStack(null)
                        .commit()
            }
            R.id.nav_report -> {
                mailLogs()
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackStackChanged() {
        checkBackStack(this)
    }

    private fun showNotRunning(){
        val snackbar = Snackbar
                .make(frame, R.string.not_running, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.not_running_fix_action) {
                    FixItFragment().show(supportFragmentManager, null)
                }
                .setActionTextColor(Color.YELLOW)
        snackbar.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
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
        text += "Device: " + Build.BRAND + " "+ Build.MODEL+ "\n"

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
                            LFMRequester (Stuff.LASTFM_SESS_AUTH, token)
                        .asAsyncTask(applicationContext)
                    "/librefm" -> {
                            LFMRequester (Stuff.LIBREFM_SESS_AUTH, token)
                        .asAsyncTask(applicationContext)
                    }
                }
            }
        }
    }

    fun showBackArrow(show: Boolean){
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

            if (show)
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            else
                drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

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
            override fun onAvailable(network: Network?) {
                isOnline = true
            }

            override fun onLost(network: Network?) {
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

    public override fun onStop() {
        unregisterReceiver(mainReceiver)
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(connectivityCb)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab_bar_visible", tab_bar.visibility)
    }

    override fun onDestroy() {
        MultiPreferences.destroyClient()
        if (!PendingScrService.mightBeRunning)
            PendingScrobblesDb.destroyInstance()
        super.onDestroy()
    }

    companion object {
        var isOnline = true

        fun checkBackStack(activity: Main){
            val appBar = activity.findViewById<StatefulAppBar>(R.id.app_bar)

            if (appBar != null) {

                if (activity.supportFragmentManager.backStackEntryCount == 0) {
                    // what the fuck, kotlin extensions? stop giving me old instances
                    if (activity.findViewById<ViewPager>(R.id.pager)?.currentItem != 2)
                        appBar.setExpanded(true, true)
                    else
                        appBar.setExpanded(false, true)
                    activity.showBackArrow(false)
                } else {
                    if (activity.supportFragmentManager.findFragmentByTag(Stuff.GET_SIMILAR)?.isVisible != true)
                        appBar.setExpanded(false, true)
                    activity.showBackArrow(true)
                }
            }
        }
    }
}
