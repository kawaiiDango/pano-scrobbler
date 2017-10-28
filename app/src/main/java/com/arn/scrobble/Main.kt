package com.arn.scrobble

import android.app.*
import android.app.Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LabeledIntent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.AppBarLayout
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
import com.arn.scrobble.db.PendingScrobblesDb
import com.arn.scrobble.pref.AppListFragment
import com.arn.scrobble.pref.PrefFragment
import com.squareup.leakcanary.LeakCanary
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.coordinator_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import java.io.File

class Main : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        FragmentManager.OnBackStackChangedListener,
        SharedPreferences.OnSharedPreferenceChangeListener{

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var pref: SharedPreferences
    private var lastDrawerOpenTime:Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!LeakCanary.isInAnalyzerProcess(this))
            LeakCanary.install(application)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

//        supportActionBar?.setDisplayShowHomeEnabled(true)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setHomeButtonEnabled(true)

        ctl.tag = " "
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        app_bar.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            var scrollRange = -1

            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.totalScrollRange
                }
                val f: Fragment? = fragmentManager.findFragmentByTag(Stuff.GET_RECENTS)
                if (f?.isVisible == true) {
                    if (scrollRange + verticalOffset == 0) {
                        ctl.title = getString(R.string.app_name)
                        heroExpanded = true
                    } else if (heroExpanded) {
                        ctl.title = ctl.tag as CharSequence
                        heroExpanded = false
                    }
                }
            }
        })


        toggle = object: ActionBarDrawerToggle(
                this, drawer_layout, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            override fun onDrawerOpened(drawerView: View?) {
                super.onDrawerOpened(drawerView)
                this@Main.onDrawerOpened()
            }
        }
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        val recentsFragment = RecentsFragment()

        if (FirstThingsFragment.checkAuthTokenExists(this) &&
                FirstThingsFragment.checkNLAccess(this)) {
            val deepLinkExtra = intent?.getIntExtra(Stuff.DEEP_LINK_KEY, 0) ?: 0


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
            onBackStackChanged()
            fragmentManager.addOnBackStackChangedListener(this)
        } else {
            fragmentManager.beginTransaction()
                    .replace(R.id.frame, FirstThingsFragment())
                    .commit()
            app_bar.setExpanded(false, true)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
//        test()
    }

    fun test (){

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
        val icon = getDrawable(R.mipmap.ic_launcher_foreground)
        icon.setColorFilter(ContextCompat.getColor(applicationContext, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)

        val nb = NotificationCompat.Builder(applicationContext)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setLargeIcon(Stuff.drawableToBitmap(icon, true))
                .setVisibility(Notification.VISIBILITY_SECRET)
                .setAutoCancel(true)
                .setShowWhen(false)
                .setUsesChronometer(true)
                .setPriority(Notification.PRIORITY_LOW)
                .addAction(R.drawable.vd_cancel, getString(R.string.unscrobble), launchIntent)
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

        nm.notify(9, n)

    }

    private fun onDrawerOpened(){
        if (drawer_layout?.isDrawerVisible(GravityCompat.START) != true ||
                System.currentTimeMillis() - lastDrawerOpenTime < Stuff.RECENTS_REFRESH_INTERVAL)
            return

        val username = if (BuildConfig.DEBUG) "nobody" else pref.getString(Stuff.USERNAME,"nobody")
        nav_name.text = username
        nav_num_scrobbles.text = getString(R.string.num_scrobbles, pref.getInt(Stuff.NUM_SCROBBLES_PREF, 0))

        nav_profile_link.setOnClickListener {
            Stuff.openInBrowser("https://www.last.fm/user/$username", this)
        }
        val picUrl = pref.getString(Stuff.PROFILE_PIC_PREF,"")
        if (picUrl != "")
            Picasso.with(this@Main)
                    .load(picUrl)
                    .placeholder(R.drawable.ic_placeholder_music)
                    .centerCrop()
                    .fit()
                    .into(nav_profile_pic, object : Callback {
                        override fun onSuccess() {
                            Palette.generateAsync((nav_profile_pic.drawable as BitmapDrawable).bitmap) { palette ->
                                val colorDomPrimary = palette.getDominantColor(
                                        ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                                (nav_header.background.mutate() as GradientDrawable)
                                        .colors = arrayOf(colorDomPrimary, Color.BLACK).toIntArray()
                            }
                        }

                        override fun onError() {
                            Stuff.log("Picasso onError drawer")
                        }
                    })

        LFMRequester(applicationContext, null).execute(Stuff.GET_DRAWER_INFO)
        lastDrawerOpenTime = System.currentTimeMillis()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        val username = pref.getString(Stuff.USERNAME,"nobody")
        when (item.itemId) {
            R.id.nav_last_week -> {
                Stuff.openInBrowser("https://www.last.fm/user/$username/listening-report/week", this)
            }
            R.id.nav_loved -> {
                Stuff.openInBrowser("https://www.last.fm/user/$username/loved", this)
            }
            R.id.nav_friends -> {
                fragmentManager.beginTransaction()
                        .hide(fragmentManager.findFragmentByTag(Stuff.GET_RECENTS))
                        .add(R.id.frame, FriendsFragment())
                        .addToBackStack(null)
                        .commit()
            }
            R.id.nav_settings -> {
                fragmentManager.beginTransaction()
                        .hide(fragmentManager.findFragmentByTag(Stuff.GET_RECENTS))
                        .add(R.id.frame, PrefFragment())
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
            Stuff.PROFILE_PIC_PREF,
            Stuff.NUM_SCROBBLES_PREF -> {
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
        Stuff.toast(this, "Generating report...")
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

        val log = Stuff.exec("logcat -d")
        val logFile = File(filesDir, "log.txt")
//        val dbFile = File(activity.filesDir, PendingScrobblesDb.tableName + ".db")
        logFile.writeText(log)
        val logUri = FileProvider.getUriForFile(this, packageName+".fileprovider", logFile)
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
            val chooser = Intent.createChooser(intents.removeAt(intents.size - 1), "Send bug report")
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
            startActivity(chooser)
        }else
            Stuff.toast(this, "There are no email clients installed.")
    }

    override fun onSupportNavigateUp(): Boolean {
        if(fragmentManager.backStackEntryCount >0 )
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
                LFMRequester(this).execute(Stuff.AUTH_FROM_TOKEN, token)
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        pref.registerOnSharedPreferenceChangeListener(this)
    }

    public override fun onPause() {
        pref.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        pref.unregisterOnSharedPreferenceChangeListener(this)
        PendingScrobblesDb.destroyInstance()
        super.onDestroy()
    }

    companion object {
        var heroExpanded = false

        fun checkBackStack(activity: Main){
            if (activity.app_bar != null) {
                activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
                if (activity.fragmentManager.backStackEntryCount == 0) {
                    activity.app_bar.setExpanded(true, true)
                    activity.drawer_layout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    activity.toggle.isDrawerIndicatorEnabled = true
                } else {
                    activity.app_bar.setExpanded(false, true)
                    activity.drawer_layout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    activity.toggle.isDrawerIndicatorEnabled = false
                }

            }
        }
    }
}
