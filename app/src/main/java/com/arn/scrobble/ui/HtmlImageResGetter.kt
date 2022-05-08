package com.arn.scrobble.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Html.ImageGetter
import androidx.core.content.ContextCompat

// https://stackoverflow.com/a/22298833/1067596
class HtmlImageResGetter(private val context: Context) : ImageGetter {
    override fun getDrawable(source: String): Drawable? {
        val id = context.resources.getIdentifier(source, "drawable", context.packageName)

        return if (id == 0) {
            // prevent a crash if the resource still can't be found
            null
        } else {
            ContextCompat.getDrawable(context, id)?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
        }
    }
}