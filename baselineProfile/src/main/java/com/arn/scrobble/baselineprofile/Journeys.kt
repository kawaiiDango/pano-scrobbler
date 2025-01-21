package com.arn.scrobble.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

object Journeys {

    fun MacrobenchmarkScope.loginIfNeeded() {
        val lastfmButton =
            device.findObject(By.res(device.currentPackageName, "button_service"))

        if (lastfmButton != null) {
            device.executeShellCommand("cmd notification allow_listener com.arn.scrobble/com.arn.scrobble.NLService")

            device.findObject(By.res(device.currentPackageName, "button_service_chooser")).click()
            Thread.sleep(1000)
            device.findObject(By.text("GNU FM")).click()
            device.findObject(By.res(device.currentPackageName, "login_textfield1_edittext"))
                .text = "test_creds_${Secrets.type}"
            device.findObject(By.res(device.currentPackageName, "login_textfield2_edittext"))
                .text = Secrets.username
            device.findObject(By.res(device.currentPackageName, "login_textfield_last_edittext"))
                .text = Secrets.sessionKey
            device.findObject(By.res(device.currentPackageName, "login_submit")).click()

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