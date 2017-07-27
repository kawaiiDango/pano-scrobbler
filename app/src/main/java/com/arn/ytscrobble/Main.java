package com.arn.ytscrobble;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import java.util.Set;


public class Main extends AppCompatActivity {
//    TextView status;
    private Fragment recentsFragment = null;
    private PrefFragment prefFragment = null;
    static boolean heroExpanded = false;

//    private NotificationReceiver nReceiver;
//    private TrackMetaListener tReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
            return;
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm/"));
                startActivity(browserIntent);
            }
        });

        final CollapsingToolbarLayout ctl  = (CollapsingToolbarLayout)findViewById(R.id.toolbar_layout);
        ctl.setTag(" ");
        AppBarLayout abl = (AppBarLayout) findViewById(R.id.app_bar);
        abl.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    ctl.setTitle(getString(R.string.app_name));
                    heroExpanded = true;
                } else if(heroExpanded) {
                    ctl.setTitle((CharSequence) ctl.getTag());//careful there should a space between double quote otherwise it wont work
                    heroExpanded = false;
                }
            }
        });

        recentsFragment = new RecentsFragment();
        getFragmentManager().beginTransaction()
            .add(R.id.frame, recentsFragment).commit();

        //status = (TextView) findViewById(R.id.status);
//        nReceiver = new NotificationReceiver();
        /*
        IntentFilter filter = new IntentFilter();
        filter.addAction(NLService.pNOTIFICATION_EVENT);
        registerReceiver(nReceiver,filter);

        IntentFilter iF = new IntentFilter();
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            if (prefFragment == null)
                prefFragment = new PrefFragment();
            getFragmentManager().beginTransaction()
                    .hide(recentsFragment)
                    .add(R.id.frame, prefFragment)
                    .addToBackStack(null)
                    .commit();

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unregisterReceiver(nReceiver);
//        unregisterReceiver(tReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean hasNotificationAccess = false;
        Set<String> packages = NotificationManagerCompat.getEnabledListenerPackages(getApplicationContext());
        for (String s : packages)
            if(s.equals(getApplicationContext().getPackageName())) {
                hasNotificationAccess = true;
                break;
            }

        if (!hasNotificationAccess) {
            Stuff.toast(getApplicationContext(), "Need Notification Access");
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
        } else {
            new Scrobbler(getApplicationContext()).execute(Stuff.CHECKAUTH);
        }

    }
}
