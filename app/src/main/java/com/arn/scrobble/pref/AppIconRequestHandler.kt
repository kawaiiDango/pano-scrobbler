package com.arn.scrobble.pref

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
            RequestHandler.Result(getFullResIcon(request.uri.toString().split(":")[1]), LoadedFrom.DISK)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun getFullResIcon(packageName: String): Bitmap {
        val info = pm.getApplicationInfo(packageName, 0)
        return Stuff.drawableToBitmap(info.loadIcon(pm), isMIUI)
    }

    companion object {
        val SCHEME_PNAME = "pname"
    }

}