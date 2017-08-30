package com.arn.ytscrobble

import android.app.Fragment
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.MenuItem

class Main : AppCompatActivity() {
    //    TextView status;
    lateinit private var recentsFragment: Fragment

    //    private NotificationReceiver nReceiver;
    //    private TrackMetaListener tReceiver;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null)
            return
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val fab = findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/"))
            startActivity(browserIntent)
        }

        val ctl = findViewById(R.id.toolbar_layout) as CollapsingToolbarLayout
        ctl.tag = " "
        val abl = findViewById(R.id.app_bar) as AppBarLayout
        abl.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            internal var scrollRange = -1

            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.totalScrollRange
                }
                if (scrollRange + verticalOffset == 0) {
                    ctl.title = getString(R.string.app_name)
                    heroExpanded = true
                } else if (heroExpanded) {
                    ctl.title = ctl.tag as CharSequence //careful there should a space between double quote otherwise it wont work
                    heroExpanded = false
                }
            }
        })

        recentsFragment = RecentsFragment()
        fragmentManager.beginTransaction()
                .add(R.id.frame, recentsFragment).commit()

        //status = (TextView) findViewById(R.id.status);
        //        nReceiver = new NotificationReceiver();
        /*
        val filter = new IntentFilter();
        filter.addAction(NLService.pNOTIFICATION_EVENT);
        registerReceiver(nReceiver,filter);

        val iF = new IntentFilter();
        iF.addAction("com.android.music.metachanged");
        iF.addAction("fm.last.android.metachanged");
        iF.addAction("com.sec.android.app.music.metachanged");
        iF.addAction("com.real.IMP.metachanged");
        iF.addAction("com.musixmatch.android.lyrify.metachanged");

        iF.addAction("com.android.music.playstatechanged");
        iF.addAction("fm.last.android.playstatechanged");
        iF.addAction("com.sec.android.app.music.playstatechanged");
        iF.addAction("com.real.IMP.playstatechanged");
        iF.addAction("com.musixmatch.android.lyrify.playstatechanged");

        iF.addAction("com.android.music.playbackcomplete");
        iF.addAction("fm.last.android.playbackcomplete");
        iF.addAction("com.sec.android.app.music.playbackcomplete");
        iF.addAction("com.real.IMP.playbackcomplete");
        iF.addAction("com.musixmatch.android.lyrify.playbackcomplete");

        tReceiver = new TrackMetaListener();
        registerReceiver(tReceiver, iF);
*/
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_settings) {
            fragmentManager.beginTransaction()
                    .hide(recentsFragment)
                    .add(R.id.frame, PrefFragment())
                    .addToBackStack(null)
                    .commit()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        //        unregisterReceiver(nReceiver);
        //        unregisterReceiver(tReceiver);
    }

    override fun onResume() {
        super.onResume()

        val packages = NotificationManagerCompat.getEnabledListenerPackages(applicationContext)
        val hasNotificationAccess = packages.any { it == applicationContext.packageName }

        if (!hasNotificationAccess) {
            Stuff.toast(applicationContext, "Need Notification Access")
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        } else {
            Scrobbler(applicationContext).execute(Stuff.CHECKAUTH)
        }

    }

    companion object {
        var heroExpanded = false
    }
}
