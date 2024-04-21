package com.arn.scrobble.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

object Journeys {

    fun MacrobenchmarkScope.loginIfNeeded() {
        val serviceChooserButton =
            device.findObject(By.res(device.currentPackageName, "button_service_chooser"))

        if (serviceChooserButton != null) {
            device.executeShellCommand("cmd notification allow_listener com.arn.scrobble/com.arn.scrobble.NLService")

            serviceChooserButton.longClick()

            device.findObject(By.res(device.currentPackageName, "testing_pass")).click()
            device.findObject(By.res(device.currentPackageName, "testing_pass")).text =
                Secrets.loginCreds
//            Create a file called Secrets.kt in the same package as this class.
//            object Secrets {
//                const val loginCreds = "username,sessionKey,"
//            }
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