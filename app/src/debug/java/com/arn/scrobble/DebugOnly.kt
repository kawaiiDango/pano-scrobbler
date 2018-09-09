/**
 * Created by arn on 02/01/2018.
 */
package com.arn.scrobble

import android.app.Application
import android.os.StrictMode
import com.squareup.leakcanary.LeakCanary

object DebugOnly {
    fun installLeakCanary(app: Application){
        if(!LeakCanary.isInAnalyzerProcess(app))
            LeakCanary.install(app)
    }

    fun strictMode() {
//        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
//                     .detectDiskReads()
//                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectActivityLeaks()
                    .detectFileUriExposure()
//                     .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build())
//        }
    }
}