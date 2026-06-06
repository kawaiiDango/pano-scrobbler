package com.arn.scrobble.widget

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.arn.scrobble.R
import com.arn.scrobble.imageloader.PanoImageLoader
import com.arn.scrobble.pref.WidgetPrefs
import com.arn.scrobble.utils.Stuff
import java.io.File

class AppWidgetImageProvider : ContentProvider() {

    private fun imagePlaceholder(ctx: Context): ParcelFileDescriptor? {
        // load the placeholder vector, render it to a bitmap, write to a temp file and return its descriptor
        val cachedFile = ctx.cacheDir.resolve("widget/placeholder.png")

        if (!cachedFile.exists()) {
            cachedFile.parentFile?.mkdirs()
            val placeholderDrawable = ctx.getDrawable(R.drawable.vd_album) ?: return null
            val bitmap = placeholderDrawable.toBitmap()
            cachedFile.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        return ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val ctx = context ?: return null

        if (mode != "r") return null

        if (uri.pathSegments.size != 3 || uri.pathSegments.getOrNull(0) != "image")
            return null

        val widgetId = uri.pathSegments.getOrNull(1)?.toIntOrNull() ?: return null
        val dataKey = uri.pathSegments.getOrNull(2)
            ?.let { WidgetPrefs.ChartsDataKey(it) }
            ?: return null
        val item = uri.getQueryParameter("item")
            ?.ifEmpty { null }
            ?.let { Stuff.myJson.decodeFromString<WidgetPrefs.ChartsWidgetListItem>(it) }
            ?: return null

        if (!item.cachedImage.isNullOrEmpty()) {

            if (item.cachedImage.startsWith("content://")) {
                // Cached image is a content URI — open it directly
                return context?.contentResolver?.openFileDescriptor(item.cachedImage.toUri(), "r")
            }

            val cachedFile = File(ctx.cacheDir, PanoImageLoader.CACHE_DIR_NAME)
                .resolve(item.cachedImage)
            if (cachedFile.exists()) {
                return ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }
        }

        // Not cached — queue it and return placeholder

        ChartsWidgetUpdaterWorker.scheduleImageFetch(ctx, widgetId, dataKey, item)
        return imagePlaceholder(ctx)
    }

    // Unused but required overrides
    override fun onCreate() = true
    override fun getType(uri: Uri) = "image/*"
    override fun query(u: Uri, p: Array<String>?, s: String?, sA: Array<String>?, so: String?) =
        null

    override fun insert(u: Uri, v: ContentValues?) = null
    override fun delete(u: Uri, s: String?, sA: Array<String>?) = 0
    override fun update(u: Uri, v: ContentValues?, s: String?, sA: Array<String>?) = 0
}