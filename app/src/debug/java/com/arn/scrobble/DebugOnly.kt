/**
 * Created by arn on 02/01/2018.
 */
package com.arn.scrobble

import android.app.Application
import com.squareup.leakcanary.LeakCanary

object DebugOnly {
    fun installLeakCanary(app: Application){
        if(!LeakCanary.isInAnalyzerProcess(app))
            LeakCanary.install(app)
    }
}