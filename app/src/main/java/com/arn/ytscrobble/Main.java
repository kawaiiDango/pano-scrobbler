package com.arn.ytscrobble;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.Set;


public class Main extends AppCompatActivity {

    TextView status;
//    private NotificationReceiver nReceiver;
//    private TrackMetaListener tReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                Intent i = new Intent(NLService.pNLS);
                i.putExtra("command","list");
                sendBroadcast(i);
            }
        });

        status = (TextView) findViewById(R.id.status);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            return true;
        } else if (id == R.id.action_reauthorize){
            getSharedPreferences(Stuff.PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .remove("sesskey")
                    .apply();
            new Scrobbler(getApplicationContext()).execute(Stuff.CHECKAUTH);
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

        new Scrobbler(getApplicationContext()).execute(Stuff.CHECKAUTH);


        boolean hasNotificationAccess = false;
        Set<String> packages = NotificationManagerCompat.getEnabledListenerPackages(getApplicationContext());
        for (String s : packages)
            if(s.equals(getApplicationContext().getPackageName())) {
                hasNotificationAccess = true;
                break;
            }

        if (!hasNotificationAccess) {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
        }

    }
/*
    class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String temp = intent.getStringExtra("notification_event") + "\n" + status.getText();
            status.setText(temp);
        }
    }
    */
}
