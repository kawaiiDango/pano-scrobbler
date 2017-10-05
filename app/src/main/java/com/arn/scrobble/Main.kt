package com.arn.scrobble

import android.app.Fragment
import android.app.Notification
import android.app.Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.app.MediaStyleMod
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import com.arn.scrobble.pref.AppListFragment
import com.arn.scrobble.pref.PrefFragment


class Main : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val ctl = findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)
        ctl.tag = " "
        val abl = findViewById<AppBarLayout>(R.id.app_bar)
        abl.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            var scrollRange = -1

            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.totalScrollRange
                }
                val f: Fragment? = fragmentManager.findFragmentByTag(Stuff.GET_RECENTS)
                if (f != null && f.isVisible) {
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

        if (FirstThingsFragment.checkAuthTokenExists(this) &&
                FirstThingsFragment.checkNLAccess(this)) {
            val deepLinkExtra = intent?.getIntExtra(Stuff.DEEP_LINK_KEY, 0) ?: 0

            val recentsFragment = RecentsFragment()
            fragmentManager.beginTransaction()
                    .replace(R.id.frame, recentsFragment, Stuff.GET_RECENTS)
                    .commit()
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
        } else {
            fragmentManager.beginTransaction()
                    .replace(R.id.frame, FirstThingsFragment())
                    .commit()
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
        val icon = getDrawable(R.drawable.ic_noti)
        icon.setColorFilter(ContextCompat.getColor(applicationContext, R.color.colorPrimary), PorterDuff.Mode.SRC_ATOP)

        val nb = NotificationCompat.Builder(applicationContext)
                .setSmallIcon(R.drawable.ic_noti)
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        var heroExpanded = false
    }
}
