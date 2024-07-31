package com.arn.scrobble.baselineprofile

import android.content.Intent
import android.net.Uri
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

object Journeys {

    fun MacrobenchmarkScope.loginIfNeeded() {
        val lastfmButton =
            device.findObject(By.res(device.currentPackageName, "button_service"))

        if (lastfmButton != null) {

            val loginIntent = Intent().apply {
                action = Intent.ACTION_VIEW
                addCategory(Intent.CATEGORY_BROWSABLE)
                `package` = device.currentPackageName
                data =
                    Uri.parse("pano-scrobbler://screen/onboarding/${Secrets.username}/${Secrets.sessionKey}")
            }
            startActivityAndWait(loginIntent)
            
            device.executeShellCommand("cmd notification allow_listener com.arn.scrobble/com.arn.scrobble.NLService")

            // click the fresh lastfm button
            device.findObject(By.res(device.currentPackageName, "button_service")).click()

            device.waitForIdle()
            Thread.sleep(1000)
            device.findObject(By.res(device.currentPackageName, "open_button")).click()
            device.waitForIdle()
            device.wait(Until.hasObject(By.desc("Done")), 10000)
            device.findObject(By.desc("Done")).click()
            device.waitForIdle()
            device.findObject(By.res(device.currentPackageName, "open_button")).click()
            device.executeShellCommand("pm grant com.arn.scrobble android.permission.POST_NOTIFICATIONS")
            device.waitForIdle()
        }
    }

    fun MacrobenchmarkScope.switchTabs() {
        device.findObject(By.desc("Scrobbles")).click()
        device.waitForIdle()
        device.findObject(By.desc("Following")).click()
        device.waitForIdle()
        device.findObject(By.desc("Charts")).click()
        device.waitForIdle()
        device.findObject(By.desc("More")).click()
        device.waitForIdle()
        Thread.sleep(1000)
        device.pressBack()
        device.setOrientationLandscape()
        device.waitForIdle()
    }
}