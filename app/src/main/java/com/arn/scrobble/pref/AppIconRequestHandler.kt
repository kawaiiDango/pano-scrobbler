package com.arn.scrobble.pref

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import java.io.IOException
import android.graphics.drawable.PictureDrawable




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
        return try {
            RequestHandler.Result(getFullResIcon(request.uri.toString().split(":")[1]), LoadedFrom.DISK)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun getFullResIcon(packageName: String): Bitmap? {
        val info = pm.getApplicationInfo(packageName, 0)
        return drawableToBitmap(info.loadIcon(pm))
    }

    companion object {

        val SCHEME_PNAME = "pname"

        fun drawableToBitmap(drawable: Drawable): Bitmap {
            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            } else if (drawable is PictureDrawable) {
                val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
                        drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawPicture(drawable.picture)
                return bitmap
            }
            var width = drawable.intrinsicWidth
            width = if (width > 0) width else 1
            var height = drawable.intrinsicHeight
            height = if (height > 0) height else 1
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }


    }

}