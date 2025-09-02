package com.arn.scrobble.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.arn.scrobble.baselineprofile.Journeys.dismissChangelogDialog
import com.arn.scrobble.baselineprofile.Journeys.loginIfNeeded
import com.arn.scrobble.baselineprofile.Journeys.switchTabs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class generates a basic startup baseline profile for the target package.
 *
 * We recommend you start with this but add important user flows to the profile to improve their performance.
 * Refer to the [baseline profile documentation](https://d.android.com/topic/performance/baselineprofiles)
 * for more information.
 *
 * You can run the generator with the Generate Baseline Profile run configuration,
 * or directly with `generateBaselineProfile` Gradle task:
 * ```
 * ./gradlew :app:generateReleaseBaselineProfile -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
 * ```
 * The run configuration runs the Gradle task and applies filtering to run only the generators.
 *
 * Check [documentation](https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args)
 * for more information about available instrumentation arguments.
 *
 * After you run the generator, you can verify the improvements running the [StartupBenchmarks] benchmark.
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        var firstIteration = true

        rule.collect("com.arn.scrobble", includeInStartupProfile = true) {
            // This block defines the app's critical user journey. Here we are interested in
            // optimizing for app startup. But you can also navigate and scroll
            // through your most important UI.

            // Ensure home before starting
            pressHome()

            // Clear app data only once (first iteration)
            if (firstIteration) {
                device.executeShellCommand("pm clear com.arn.scrobble")
                // Optional small delay to let the system finish clearing
                Thread.sleep(500)
                firstIteration = false
            }

            startActivityAndWait()
            device.setOrientationPortrait()

            if (iteration == 1) {
                loginIfNeeded()
                dismissChangelogDialog()
            }

            Thread.sleep(2000)
            switchTabs()
            Thread.sleep(1000)

            // Write more interactions to optimize advanced journeys of your app.
            // For example:
            // 1. Wait until the content is asynchronously loaded
            // 2. Scroll the feed content
            // 3. Navigate to detail screen

            // Check UiAutomator documentation for more information how to interact with the app.
            // https://d.android.com/training/testing/other-components/ui-automator
        }
    }
}