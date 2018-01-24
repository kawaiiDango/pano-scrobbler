package com.arn.scrobble

import android.animation.ValueAnimator
import android.app.*
import android.app.Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES
import android.content.*
import android.content.pm.LabeledIntent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v4.media.app.MediaStyleMod
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.graphics.Palette
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.arn.scrobble.db.PendingScrobblesDb
import com.arn.scrobble.pref.AppListFragment
import com.arn.scrobble.pref.PrefFragment
import com.arn.scrobble.ui.MyAppBarLayout
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.coordinator_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import org.codechimp.apprater.AppRater
import java.io.File

class Main : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        FragmentManager.OnBackStackChangedListener,
        SharedPreferences.OnSharedPreferenceChangeListener{

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var pref: SharedPreferences
    private var lastDrawerOpenTime:Long = 0
    private var backArrowShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        DebugOnly.installLeakCanary(application)
        Stuff.timeIt("onCreate start")
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        Stuff.timeIt("onCreate setContentView")
        setSupportActionBar(toolbar)

        ctl.tag = getString(R.string.app_name)
        ctl.title = " "

        pref = PreferenceManager.getDefaultSharedPreferences(this)
        app_bar.setOnStateChangeListener { state ->
            val f: Fragment? = fragmentManager.findFragmentByTag(Stuff.GET_RECENTS)
            if (f?.isVisible == true) {
                when (state) {
                    MyAppBarLayout.State.EXPANDED, MyAppBarLayout.State.IDLE -> {
                        ctl.title = " " //ctl.tag as CharSequence
//                        f.recents_list?.header_text?.visibility = View.VISIBLE
                    }
                    MyAppBarLayout.State.COLLAPSED -> {
                        ctl.title = ctl.tag as CharSequence
                    }
                }
            }
        }

        toggle = object: ActionBarDrawerToggle(
                this, drawer_layout, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                this@Main.onDrawerOpened()
            }
        }
        drawer_layout.addDrawerListener(toggle)
        nav_view.setNavigationItemSelectedListener(this)

        if (FirstThingsFragment.checkAuthTokenExists(this) &&
                FirstThingsFragment.checkNLAccess(this)) {
            if (savedInstanceState == null) {
                val deepLinkExtra = intent?.getIntExtra(Stuff.DEEP_LINK_KEY, 0) ?: 0
                val recentsFragment = RecentsFragment()

                fragmentManager.beginTransaction()
                        .replace(R.id.frame, recentsFragment, Stuff.GET_RECENTS)
                        .commit()
//            fragmentManager.beginTransaction()
//                    .hide(recentsFragment)
//                    .add(R.id.frame, FriendsFragment())
//                    .addToBackStack(null)
//                    .commit()
                if (deepLinkExtra == Stuff.DL_SETTINGS || intent?.categories?.contains(INTENT_CATEGORY_NOTIFICATION_PREFERENCES) == true)
                    fragmentManager.beginTransaction()
                            .hide(recentsFragment)
                            .add(R.id.frame, PrefFragment())
                            .addToBackStack(null)
                            .commit()
                else if (deepLinkExtra == Stuff.DL_APP_LIST)
                    fragmentManager.beginTransaction()
                            .hide(recentsFragment)
                            .add(R.id.frame, AppListFragment())
                            .addToBackStack(null)
                            .commit()
                else {
                    AppRater.app_launched(this)
                    NLService.ensureServiceRunning(this)
                }
            }
            onBackStackChanged()
            fragmentManager.addOnBackStackChangedListener(this)
        } else {
            if (savedInstanceState == null) {
                fragmentManager.beginTransaction()
                        .replace(R.id.frame, FirstThingsFragment())
                        .commit()
            }
            app_bar.setExpanded(false, true)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
//        test()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
        Stuff.setAppBarHeight(this)
        Stuff.timeIt("onPostCreate")
    }

    fun test (){
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

        val nb = NotificationCompat.Builder(applicationContext)
                .setSmallIcon(R.drawable.ic_noti)
                .setLargeIcon(Stuff.drawableToBitmap(icon))
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setAutoCancel(true)
                .setShowWhen(false)
                .setUsesChronometer(true)
                .setPriority(Notification.PRIORITY_LOW)
                .addAction(R.drawable.vd_undo, getString(R.string.unscrobble), launchIntent)
                .addAction(R.drawable.vd_check, getString(R.string.unscrobble), launchIntent)
                .setContentTitle("setContentTitle")
                .setContentText("longDescription")
                .setSubText("setSubText")
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
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
//        rv.setImageViewResource(resId, R.drawable.vd_cancel)
*/
        nm.notify(9, n)

    }

    private fun onDrawerOpened(){
        if (drawer_layout?.isDrawerVisible(GravityCompat.START) != true ||
                System.currentTimeMillis() - lastDrawerOpenTime < Stuff.RECENTS_REFRESH_INTERVAL)
            return

        val username = if (BuildConfig.DEBUG) "nobody" else pref.getString(Stuff.USERNAME,"nobody")
        nav_name.text = username
        val num = pref.getInt(Stuff.PREF_NUM_SCROBBLES, 0)
        nav_num_scrobbles.text = resources.getQuantityString(R.plurals.num_scrobbles, num, num)

        nav_profile_link.setOnClickListener { v:View ->
            Stuff.openInBrowser("https://www.last.fm/user/$username", this, v)
        }
        val picUrl = pref.getString(Stuff.PREF_PROFILE_PIC,"")
        if (picUrl != "")
            Picasso.with(this@Main)
                    .load(picUrl)
                    .placeholder(R.drawable.ic_placeholder_music)
                    .centerCrop()
                    .fit()
                    .into(nav_profile_pic, object : Callback {
                        override fun onSuccess() {
                            Palette.generateAsync((nav_profile_pic.drawable as BitmapDrawable).bitmap) { palette ->
                                try {
                                    val colorDomPrimary = palette.getDominantColor(
                                            ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                                    (nav_header.background.mutate() as GradientDrawable)
                                            .colors = arrayOf(colorDomPrimary, Color.BLACK).toIntArray()
                                } catch (e: IllegalStateException){

                                }
                            }
                        }

                        override fun onError() {
                            Stuff.log("Picasso onError drawer")
                        }
                    })

        LFMRequester(applicationContext, Stuff.GET_DRAWER_INFO).inAsyncTask()
        lastDrawerOpenTime = System.currentTimeMillis()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val username = pref.getString(Stuff.USERNAME,"nobody")
        when (item.itemId) {
            R.id.nav_last_week -> {
                Stuff.openInBrowser("https://www.last.fm/user/$username/listening-report/week", this, frame, 10, 200)
            }
            R.id.nav_loved -> {
                Stuff.openInBrowser("https://www.last.fm/user/$username/loved", this, frame, 10, 200)
            }
            R.id.nav_friends -> {
                fragmentManager.beginTransaction()
                        .replace(R.id.frame, FriendsFragment(), Stuff.GET_FRIENDS)
//                        .hide(fragmentManager.findFragmentByTag(Stuff.GET_RECENTS))
//                        .add(R.id.frame, FriendsFragment())
                        .addToBackStack(null)
                        .commit()
            }
            R.id.nav_settings -> {
                fragmentManager.beginTransaction()
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

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
        when(key){
            Stuff.PREF_PROFILE_PIC,
            Stuff.PREF_NUM_SCROBBLES -> {
                lastDrawerOpenTime = 0
                onDrawerOpened()
            }
            else -> {
                val intent = Intent(NLService.iPREFS_CHANGED)
                        .putExtra("key", key)
                val value = sp.all[key]
                when (value) {
                    is Int -> intent.putExtra("value", value)
                    is Float -> intent.putExtra("value", value)
                    is Long -> intent.putExtra("value", value)
                    is Boolean -> intent.putExtra("value", value)
                    is String -> intent.putExtra("value", value)
                    is Set<*> -> intent.putExtra("value", (value as Set<String>).toTypedArray())
                    else -> {
                        Stuff.log("unknown prefs type")
                        return
                    }
                }
                sendBroadcast(intent)
            }
        }
    }

    override fun onBackStackChanged() {
        checkBackStack(this)
    }

    private fun mailLogs(){
        Stuff.toast(this, getString(R.string.generating_report))
        var text = ""
        text += getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME+ "\n"
        text += "Android " + Build.VERSION.RELEASE+ "\n"
        text += "ROM: " + Build.DISPLAY+ "\n"
        text += "Device: " + Build.BRAND + " "+ Build.MODEL+ "\n"

        val mi = ActivityManager.MemoryInfo()
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        val megs = mi.totalMem / 1048576L
        text += "RAM: " + megs + "M \n"

        val dm = resources.displayMetrics

        text += "Screen: " + dm.widthPixels + " x " + dm.heightPixels + ",  " + dm.densityDpi + " DPI\n"
        text += "------------------------\n\n[how did this happen?]\n"
        //keep the email in english

        val log = Stuff.exec("logcat -d")
        val logFile = File(filesDir, "log.txt")
//        val dbFile = File(activity.filesDir, PendingScrobblesDb.tableName + ".db")
        logFile.writeText(log)
        val logUri = FileProvider.getUriForFile(this, "com.arn.scrobble.fileprovider", logFile)
//        activity.getDatabasePath(PendingScrobblesDb.tableName)
//                .copyTo(dbFile, true)
//        val dbUri = FileProvider.getUriForFile(activity, activity.packageName+".fileprovider", dbFile)

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
            fragmentManager.popBackStack()
        else
            drawer_layout.openDrawer(GravityCompat.START)
        return true
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.data?.isHierarchical == true) {
            val token = intent.data.getQueryParameter("token")
            if (token != null){
                Stuff.log("onNewIntent got token")
                LFMRequester(this, Stuff.AUTH_FROM_TOKEN, token).inAsyncTask()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        Stuff.setAppBarHeight(this)
    }

    fun showBackArrow(show: Boolean = true){
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
            backArrowShown = show
        }
    }

    private val mainReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                CONNECTIVITY_ACTION -> isOnline = Stuff.getOnlineStatus(context)
                NLService.pWHITELIST, NLService.pBLACKLIST -> {
                    val wSet = pref.getStringSet(Stuff.PREF_WHITELIST, mutableSetOf())
                    val bSet = pref.getStringSet(Stuff.PREF_BLACKLIST, mutableSetOf())

                    if (intent.action == NLService.pWHITELIST)
                        wSet.add(intent.getStringExtra("packageName"))
                    else {
                        bSet.add(intent.getStringExtra("packageName"))
                    }
                    bSet.removeAll(wSet) //whitelist takes over blacklist for conflicts
                    pref.edit()
                            .putStringSet(Stuff.PREF_WHITELIST, wSet)
                            .putStringSet(Stuff.PREF_BLACKLIST,  bSet)
                            .apply()
                }
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        pref.registerOnSharedPreferenceChangeListener(this)
        val iF = IntentFilter()
        iF.addAction(CONNECTIVITY_ACTION)
        iF.addAction(NLService.pBLACKLIST)
        iF.addAction(NLService.pWHITELIST)
        registerReceiver(mainReceiver, iF)
        isOnline = Stuff.getOnlineStatus(this)
    }

    public override fun onStop() {
        pref.unregisterOnSharedPreferenceChangeListener(this)
        unregisterReceiver(mainReceiver)
        super.onStop()
    }

    override fun onDestroy() {
        pref.unregisterOnSharedPreferenceChangeListener(this)
        PendingScrobblesDb.destroyInstance()
        super.onDestroy()
    }

    companion object {
        var isOnline = true

        fun checkBackStack(activity: Main){
            if (activity.app_bar != null) {
                activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
                if (activity.fragmentManager.backStackEntryCount == 0) {
                    activity.app_bar.setExpanded(true, true)
                    activity.drawer_layout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    activity.showBackArrow(false)
                } else {
                    if (activity.fragmentManager.findFragmentByTag(Stuff.GET_SIMILAR)?.isVisible != true)
                        activity.app_bar.setExpanded(false, true)
                    activity.drawer_layout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    activity.showBackArrow(true)
                }
            }
        }
    }
}
