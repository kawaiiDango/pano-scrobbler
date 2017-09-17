package com.arn.scrobble.pref

import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.text.TextUtils
import com.squareup.picasso.Picasso.LoadedFrom
import android.app.ActivityManager
import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import java.io.IOException


/**
 * Created by arn on 17/09/2017.
 */
class AppIconRequestHandler(context: Context) : RequestHandler() {

    private val pm: PackageManager
    private val dpi: Int

    init {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        dpi = am.launcherLargeIconDensity
        pm = context.packageManager
    }

    override fun canHandleRequest(data: Request): Boolean {
        return data.uri != null && TextUtils.equals(data.uri.scheme, SCHEME_PNAME)
    }

    @Throws(IOException::class)
    override fun load(request: Request, networkPolicy: Int): RequestHandler.Result? {
        try {
            return RequestHandler.Result(getFullResIcon(request.uri.toString().split(":")[1]), LoadedFrom.DISK)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }

    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun getFullResIcon(packageName: String): Bitmap? {
        val info = pm.getApplicationInfo(packageName, 0)
        try {
            return drawableToBitmap(info.loadIcon(pm))!!
        } catch (ignored: PackageManager.NameNotFoundException) {
            return null
        }
    }

    companion object {

        val SCHEME_PNAME = "pname"

        fun drawableToBitmap(drawable: Drawable): Bitmap? {
            var bitmap: Bitmap? = null

            if (drawable is BitmapDrawable) {
                if (drawable.bitmap != null) {
                    return drawable.bitmap
                }
            }

            if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
            } else {
                bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            }

            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }


    }

}