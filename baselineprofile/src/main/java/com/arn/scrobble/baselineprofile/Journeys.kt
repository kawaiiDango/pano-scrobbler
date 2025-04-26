package com.arn.scrobble.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

object Journeys {

    private const val TIMEOUT = 10000L

    fun MacrobenchmarkScope.loginIfNeeded() {
        val lastfmButton = device.wait(Until.hasObject(By.text("Last.fm")), TIMEOUT)

        if (lastfmButton) {
            device.executeShellCommand("cmd notification allow_listener com.arn.scrobble/com.arn.scrobble.main.NLService")

            device.findObject(By.res("login_type_dropdown")).click()
            device.wait(Until.hasObject(By.text("Last.fm-like instance")), TIMEOUT)
            device.findObject(By.text("Last.fm-like instance")).click()
            device.findObject(By.res("login_url")).text = "test_creds_${Secrets.type}"
            device.findObject(By.res("login_username")).text = Secrets.username
            device.findObject(By.res("login_password")).text = Secrets.sessionKey
            device.findObject(By.text("Verify")).click()
            Thread.sleep(1000)
            device.findObject(By.res("button_stepper_open")).click()
            device.waitForIdle()
            device.wait(Until.hasObject(By.desc("Done")), TIMEOUT)
            device.findObject(By.desc("Done")).click()
            Thread.sleep(1000)
            device.findObject(By.res("button_stepper_open")).click()
            device.executeShellCommand("pm grant com.arn.scrobble android.permission.POST_NOTIFICATIONS")
            device.waitForIdle()
        }
    }

    fun MacrobenchmarkScope.switchTabs() {
        device.wait(Until.hasObject(By.text("Scrobbles")), TIMEOUT)

        device.findObject(By.text("Scrobbles")).click()
        device.waitForIdle()
        device.findObject(By.text("Following")).click()
        device.waitForIdle()
        device.findObject(By.text("Charts")).click()
        device.waitForIdle()
        device.findObject(By.text(Secrets.username)).click()
        device.waitForIdle()
        Thread.sleep(1000)
        device.pressBack()
        device.setOrientationLandscape()
        device.waitForIdle()
    }
}