package com.arn.ytscrobble;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.Menu;
import android.view.MenuInflater;

/**
 * Created by arn on 09/07/2017.
 */

public class PrefFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        Preference reauth = findPreference("reauth");
        reauth.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                //open browser or intent here
                getPreferenceManager().getSharedPreferences()
                        .edit()
                        .remove("sesskey")
                        .apply();
                new Scrobbler(getContext()).execute(Stuff.CHECKAUTH);
                return true;
            }
        });
        Preference about = findPreference("about");
        String pName = getContext().getPackageName();
        try {
            PackageInfo pInfo = getContext(). getPackageManager().getPackageInfo(pName, 0);
            String version = pInfo.versionName;
            about.setTitle(pName + " v" + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        DropDownPreference delaySecs = (DropDownPreference) findPreference("delay_secs");
        delaySecs.setSummary(delaySecs.getEntry());

        DropDownPreference delayPer = (DropDownPreference) findPreference("delay_per");
        delayPer.setSummary(delayPer.getEntry());

    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equals("master")) {
            boolean val = sp.getBoolean("master", true);
            SwitchPreferenceCompat mxm = (SwitchPreferenceCompat) findPreference("scrobble_mxmFloatingLyrics"),
                    yt = (SwitchPreferenceCompat) findPreference("scrobble_youtube");
            mxm.setChecked(val);
            yt.setChecked(val);
        } else if (key.equals("delay_secs") || key.equals("delay_per")) {
            findPreference(key).setSummary(sp.getString(key,""));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

}
