/**
 * Created by arn on 02/01/2018.
 */
package com.arn.scrobble

import android.app.Application

object DebugOnly {
    fun installLeakCanary(app: Application){}
    fun strictMode(){}
}