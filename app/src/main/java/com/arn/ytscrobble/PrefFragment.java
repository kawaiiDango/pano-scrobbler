package com.arn.ytscrobble;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;

/**
 * Created by arn on 09/07/2017.
 */

public class PrefFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);

        ((CollapsingToolbarLayout)getActivity().findViewById(R.id.toolbar_layout))
                .setTitle(getString(R.string.action_settings));
        AppBarLayout vg = (AppBarLayout) getActivity().findViewById(R.id.app_bar);
        vg.setExpanded(false, true);

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
                new Scrobbler(getActivity()).execute(Stuff.CHECKAUTH);
                return true;
            }
        });
        Preference about = findPreference("about");
        String pName = getActivity().getPackageName();
        try {
            PackageInfo pInfo = getActivity(). getPackageManager().getPackageInfo(pName, 0);
            String version = pInfo.versionName;
            about.setTitle(pName + " v" + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equals("master")) {
            boolean val = sp.getBoolean("master", true);
            SwitchPreference mxm = (SwitchPreference) findPreference("scrobble_mxmFloatingLyrics"),
                    yt = (SwitchPreference) findPreference("scrobble_youtube");
            mxm.setChecked(val);
            yt.setChecked(val);
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
