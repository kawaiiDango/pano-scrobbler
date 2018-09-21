package com.arn.scrobble.pref

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.text.TextUtils
import com.arn.scrobble.Stuff
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import java.io.IOException


/**
 * Created by arn on 17/09/2017.
 */
class AppIconRequestHandler(context: Context) : RequestHandler() {

    private val pm: PackageManager
    private val dpi: Int
    private val isMIUI:Boolean

    init {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        dpi = am.launcherLargeIconDensity
        pm = context.packageManager
        isMIUI = Stuff.isMiui(context)
    }

    override fun canHandleRequest(data: Request): Boolean {
        return data.uri != null && TextUtils.equals(data.uri.scheme, SCHEME_PNAME)
    }

    @Throws(IOException::class)
    override fun load(request: Request, networkPolicy: Int): RequestHandler.Result? {
        return try {
            val info = pm.getApplicationInfo(request.uri.toString().split(":")[1], 0)
            val b = Stuff.drawableToBitmap(info.loadIcon(pm), request.targetWidth, request.targetHeight, isMIUI)
            RequestHandler.Result(b, LoadedFrom.DISK)
        } catch (e: Exception) { //catch miui security exceptions too
            null
        }
    }

    companion object {
        const val SCHEME_PNAME = "pname"
    }

}