package org.codechimp.apprater

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.Main
import com.arn.scrobble.R
import com.arn.scrobble.pref.MultiPreferences
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

object AppRater {
    // Preference Constants
    val PREF_NAME = "apprater"
    val SCROBBLE_COUNT = "scrobble_count"
    private val PREF_FIRST_LAUNCHED = "date_firstlaunch"
    private val PREF_DONT_SHOW_AGAIN = "dontshowagain"
    private val PREF_REMIND_LATER = "remindmelater"
    private val PREF_APP_VERSION_NAME = "app_version_name"
    private val PREF_APP_VERSION_CODE = "app_version_code"

    private val DAYS_UNTIL_PROMPT = 3
    val MIN_SCROBBLES_UNTIL_PROMPT = 15
    private var DAYS_UNTIL_PROMPT_FOR_REMIND_LATER = 3
    private var LAUNCHES_UNTIL_PROMPT_FOR_REMIND_LATER = 15
    private var hideNoButton: Boolean = false
    private var isVersionNameCheckEnabled: Boolean = false
    private var isVersionCodeCheckEnabled: Boolean = false
    private var isCancelable = true

    /**
     * Get the currently set Market
     *
     * @return market
     */
    /**
     * Set an alternate Market, defaults to Google Play
     *
     * @param market
     */
    var market: Market = GoogleMarket()

    /**
     * Decides if the version name check is active or not
     *
     * @param versionNameCheck
     */
    fun setVersionNameCheckEnabled(versionNameCheck: Boolean) {
        isVersionNameCheckEnabled = versionNameCheck
    }

    /**
     * Decides if the version code check is active or not
     *
     * @param versionCodeCheck
     */
    fun setVersionCodeCheckEnabled(versionCodeCheck: Boolean) {
        isVersionCodeCheckEnabled = versionCodeCheck
    }

    /**
     * sets number of day until rating dialog pops up for next time when remind
     * me later option is chosen
     *
     * @param daysUntilPromt
     */
    fun setNumDaysForRemindLater(daysUntilPromt: Int) {
        DAYS_UNTIL_PROMPT_FOR_REMIND_LATER = daysUntilPromt
    }

    /**
     * sets the number of launches until the rating dialog pops up for next time
     * when remind me later option is chosen
     *
     * @param launchesUntilPrompt
     */
    fun setNumLaunchesForRemindLater(launchesUntilPrompt: Int) {

        LAUNCHES_UNTIL_PROMPT_FOR_REMIND_LATER = launchesUntilPrompt
    }

    /**
     * decides if No thanks button appear in dialog or not
     *
     * @param isNoButtonVisible
     */
    fun setDontRemindButtonVisible(isNoButtonVisible: Boolean) {
        AppRater.hideNoButton = isNoButtonVisible
    }

    /**
     * sets whether the rating dialog is cancelable or not, default is true.
     *
     * @param cancelable
     */
    fun setCancelable(cancelable: Boolean) {
        isCancelable = cancelable
    }

    /**
     * Call this method at the end of your OnCreate method to determine whether
     * to show the rate prompt using the specified or default day, launch count
     * values with additional day and launch parameter for remind me later option
     * and checking if the version is changed or not
     *
     * @param context
     * @param daysUntilPrompt
     * @param launchesUntilPrompt
     * @param daysForRemind
     * @param launchesForRemind
     */
    fun app_launched(context: Context, daysUntilPrompt: Int, launchesUntilPrompt: Int, daysForRemind: Int, launchesForRemind: Int) {
        setNumDaysForRemindLater(daysForRemind)
        setNumLaunchesForRemindLater(launchesForRemind)
        app_launched(context, daysUntilPrompt, launchesUntilPrompt)
    }

    /**
     * Call this method at the end of your OnCreate method to determine whether
     * to show the rate prompt
     *
     * @param context
     * @param daysUntilPrompt
     * @param scrobblesUntilPrompt
     */
    fun app_launched(context: Context, daysUntilPrompt: Int = DAYS_UNTIL_PROMPT,
                     scrobblesUntilPrompt: Int = MIN_SCROBBLES_UNTIL_PROMPT): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val days: Int
        val scrobbles: Int
        if (isVersionNameCheckEnabled) {
            if (BuildConfig.VERSION_NAME != prefs.getString(PREF_APP_VERSION_NAME, "none")) {
                editor.putString(PREF_APP_VERSION_NAME, BuildConfig.VERSION_NAME)
                resetData(context)
                editor.apply()
            }
        }
        if (isVersionCodeCheckEnabled) {
            if (BuildConfig.VERSION_CODE != prefs.getInt(PREF_APP_VERSION_CODE, -1)) {
                editor.putInt(PREF_APP_VERSION_CODE, BuildConfig.VERSION_CODE)
                resetData(context)
                editor.apply()
            }
        }
        if (prefs.getBoolean(PREF_DONT_SHOW_AGAIN, false)) {
            return false
        } else if (prefs.getBoolean(PREF_REMIND_LATER, false)) {
            days = DAYS_UNTIL_PROMPT_FOR_REMIND_LATER
            scrobbles = LAUNCHES_UNTIL_PROMPT_FOR_REMIND_LATER
        } else {
            days = daysUntilPrompt
            scrobbles = scrobblesUntilPrompt
        }

        val scrobble_count = MultiPreferences(context)
            .getInt(SCROBBLE_COUNT, 0)
        // Get date of first launch
        var date_firstLaunch: Long? = prefs.getLong(PREF_FIRST_LAUNCHED, 0)
        if (date_firstLaunch == 0L) {
            date_firstLaunch = System.currentTimeMillis()
            editor.putLong(PREF_FIRST_LAUNCHED, date_firstLaunch)
        }
        // Wait for at least the number of scrobbles && the number of days used
        // until prompt
        val shouldShowPrompt = scrobble_count >= scrobbles && System.currentTimeMillis() >= date_firstLaunch!! + days * 24 * 60 * 60 * 1000
        
        if (shouldShowPrompt)
            showRateSnackbar(context, editor)
        editor.apply()
        
        return shouldShowPrompt
    }

    /**
     * Call this method directly if you want to force a rate prompt, useful for
     * testing purposes
     *
     * @param context
     */
    fun showRateSnackbar(context: Context) {
        showRateSnackbar(context, null)
    }

    /**
     * Call this method directly to go straight to play store listing for rating
     *
     * @param context
     */
    fun rateNow(context: Context) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, market.getMarketURI(context)))
        } catch (activityNotFoundException1: ActivityNotFoundException) {
            Timber.tag("AppRater").e("Market Intent not found")
        }

    }

    fun setPackageName(packageName: String) {
        AppRater.market.overridePackageName(packageName)
    }

    /**
     * The meat of the library, actually shows the rate prompt dialog
     */
    private fun showRateSnackbar(context: Context, editor: SharedPreferences.Editor?) {
        val coordinatorLayout = (context as Activity).findViewById<View>(R.id.frame)
        val snackbar = Snackbar
                .make(coordinatorLayout, R.string.rate_msg, Snackbar.LENGTH_INDEFINITE)
                .setAction("\uD83C\uDF1F " + context.getString(R.string.rate_action)) {
                    rateNow(context)
                    if (editor != null) {
                        editor.putBoolean(PREF_DONT_SHOW_AGAIN, true)
                        editor.apply()
                    }
                }
                .addCallback(object : Snackbar.Callback() {
                    override fun onShown(sb: Snackbar?) {
                        super.onShown(sb)
                        if (sb != null && Main.isTV)
                            sb.view.postDelayed({
                                sb.view.findViewById<View>(com.google.android.material.R.id.snackbar_action)
                                        .requestFocus()
                            }, 200)
                    }
                })
/*      never show
                if (editor != null) {
                    editor.putBoolean(PREF_DONT_SHOW_AGAIN, true);
                    editor.putBoolean(PREF_REMIND_LATER, false);
                    long date_firstLaunch = System.currentTimeMillis();
                    editor.putLong(PREF_FIRST_LAUNCHED, date_firstLaunch);
                    editor.putLong(PREF_LAUNCH_COUNT, 0);
                    editor.apply();
                }
 */
        snackbar.show()

        if (editor != null) {
            val date_firstLaunch = System.currentTimeMillis()
            editor.putLong(PREF_FIRST_LAUNCHED, date_firstLaunch)
            editor.putBoolean(PREF_REMIND_LATER, true)
            editor.putBoolean(PREF_DONT_SHOW_AGAIN, false)
            editor.apply()
        }
    }

    fun resetData(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(PREF_DONT_SHOW_AGAIN, false)
        editor.putBoolean(PREF_REMIND_LATER, false)
        MultiPreferences(context)
            .putInt(SCROBBLE_COUNT, 0)
        val date_firstLaunch = System.currentTimeMillis()
        editor.putLong(PREF_FIRST_LAUNCHED, date_firstLaunch)
        editor.apply()
    }

    fun incrementScrobbleCount(defPref: SharedPreferences) {
        val scrobbles = defPref.getInt(SCROBBLE_COUNT, 0)
        if (scrobbles < MIN_SCROBBLES_UNTIL_PROMPT)
            defPref.edit().putInt(SCROBBLE_COUNT, scrobbles + 1).apply()
    }
}
/**
 * Call this method at the end of your OnCreate method to determine whether
 * to show the rate prompt using the specified or default day, launch count
 * values and checking if the version is changed or not
 *
 * @param context
 */
