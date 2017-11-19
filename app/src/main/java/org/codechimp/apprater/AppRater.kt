package org.codechimp.apprater

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import com.arn.scrobble.R

object AppRater {
    // Preference Constants
    private val PREF_NAME = "apprater"
    private val PREF_LAUNCH_COUNT = "launch_count"
    private val PREF_FIRST_LAUNCHED = "date_firstlaunch"
    private val PREF_DONT_SHOW_AGAIN = "dontshowagain"
    private val PREF_REMIND_LATER = "remindmelater"
    private val PREF_APP_VERSION_NAME = "app_version_name"
    private val PREF_APP_VERSION_CODE = "app_version_code"

    private val DAYS_UNTIL_PROMPT = 3
    private val LAUNCHES_UNTIL_PROMPT = 7
    private var DAYS_UNTIL_PROMPT_FOR_REMIND_LATER = 3
    private var LAUNCHES_UNTIL_PROMPT_FOR_REMIND_LATER = 7
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
     * @param launchesUntilPrompt
     */
    @JvmOverloads
    fun app_launched(context: Context, daysUntilPrompt: Int = DAYS_UNTIL_PROMPT, launchesUntilPrompt: Int = LAUNCHES_UNTIL_PROMPT) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val ratingInfo = ApplicationRatingInfo.createApplicationInfo(context)
        val days: Int
        val launches: Int
        if (isVersionNameCheckEnabled) {
            if (ratingInfo.applicationVersionName != prefs.getString(PREF_APP_VERSION_NAME, "none")) {
                editor.putString(PREF_APP_VERSION_NAME, ratingInfo.applicationVersionName)
                resetData(context)
                commitOrApply(editor)
            }
        }
        if (isVersionCodeCheckEnabled) {
            if (ratingInfo.applicationVersionCode != prefs.getInt(PREF_APP_VERSION_CODE, -1)) {
                editor.putInt(PREF_APP_VERSION_CODE, ratingInfo.applicationVersionCode)
                resetData(context)
                commitOrApply(editor)
            }
        }
        if (prefs.getBoolean(PREF_DONT_SHOW_AGAIN, false)) {
            return
        } else if (prefs.getBoolean(PREF_REMIND_LATER, false)) {
            days = DAYS_UNTIL_PROMPT_FOR_REMIND_LATER
            launches = LAUNCHES_UNTIL_PROMPT_FOR_REMIND_LATER
        } else {
            days = daysUntilPrompt
            launches = launchesUntilPrompt
        }

        // Increment launch counter
        val launch_count = prefs.getLong(PREF_LAUNCH_COUNT, 0) + 1
        editor.putLong(PREF_LAUNCH_COUNT, launch_count)
        // Get date of first launch
        var date_firstLaunch: Long? = prefs.getLong(PREF_FIRST_LAUNCHED, 0)
        if (date_firstLaunch == 0.toLong()) {
            date_firstLaunch = System.currentTimeMillis()
            editor.putLong(PREF_FIRST_LAUNCHED, date_firstLaunch)
        }
        // Wait for at least the number of launches or the number of days used
        // until prompt
        if (launch_count >= launches || System.currentTimeMillis() >= date_firstLaunch!! + days * 24 * 60 * 60 * 1000) {
            showRateSnackbar(context, editor)
        }
        commitOrApply(editor)
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
            Log.e(AppRater::class.java.simpleName, "Market Intent not found")
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
                .setAction("\uD83C\uDF1F " + context.getString(R.string.rate_link), {
                    rateNow(context)
                    if (editor != null) {
                        editor.putBoolean(PREF_DONT_SHOW_AGAIN, true);
                        commitOrApply(editor)
                    }
                })
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, @DismissEvent event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        if (event == Snackbar.Callback.DISMISS_EVENT_SWIPE || event == Snackbar.Callback.DISMISS_EVENT_MANUAL) {
                            if (editor != null) {
                                val date_firstLaunch = System.currentTimeMillis()
                                editor.putLong(PREF_FIRST_LAUNCHED, date_firstLaunch)
                                editor.putLong(PREF_LAUNCH_COUNT, 0)
                                editor.putBoolean(PREF_REMIND_LATER, true)
                                editor.putBoolean(PREF_DONT_SHOW_AGAIN, false)
                                commitOrApply(editor)
                            }
                        }
                    }
                })
/*      never show
                if (editor != null) {
                    editor.putBoolean(PREF_DONT_SHOW_AGAIN, true);
                    editor.putBoolean(PREF_REMIND_LATER, false);
                    long date_firstLaunch = System.currentTimeMillis();
                    editor.putLong(PREF_FIRST_LAUNCHED, date_firstLaunch);
                    editor.putLong(PREF_LAUNCH_COUNT, 0);
                    commitOrApply(editor);
                }
 */
        snackbar.show()
    }

    private fun commitOrApply(editor: SharedPreferences.Editor) {
        editor.apply()
    }

    fun resetData(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(PREF_DONT_SHOW_AGAIN, false)
        editor.putBoolean(PREF_REMIND_LATER, false)
        editor.putLong(PREF_LAUNCH_COUNT, 0)
        val date_firstLaunch = System.currentTimeMillis()
        editor.putLong(PREF_FIRST_LAUNCHED, date_firstLaunch)
        commitOrApply(editor)
    }
}
/**
 * Call this method at the end of your OnCreate method to determine whether
 * to show the rate prompt using the specified or default day, launch count
 * values and checking if the version is changed or not
 *
 * @param context
 */
