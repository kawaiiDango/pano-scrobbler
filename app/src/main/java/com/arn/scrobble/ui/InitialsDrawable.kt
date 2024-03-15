package com.arn.scrobble.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.utils.UiUtils
import com.google.android.material.color.MaterialColors
import kotlin.math.min

class InitialsDrawable(context: Context, name: String, colorFromHash: Boolean = true) : Drawable() {

    private val initials = name[0].uppercase()
    private val bgColor by lazy {
        if (!colorFromHash)
//            Color.TRANSPARENT
            MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, null)
        else
            UiUtils.getMatColor(
                context, name.hashCode(),
                if (context.resources.getBoolean(R.bool.is_dark))
                    "600"
                else
                    "400"
            )
    }

    private val fgColor by lazy {
        if (!colorFromHash)
            MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, null)
        else
            UiUtils.getMatColor(
                context, name.hashCode(),
                if (context.resources.getBoolean(R.bool.is_dark))
                    "100"
                else
                    "900"
            )
    }

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgColor
            style = Paint.Style.FILL_AND_STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }

    private val textBounds = Rect()


    constructor(context: Context, userCached: UserCached) :
            this(
                context,
                userCached.realname.ifEmpty { userCached.name }
            )

    constructor(context: Context, user: User) :
            this(context, if (!user.realname.isNullOrEmpty()) user.realname else user.name)

    override fun draw(canvas: Canvas) {
        // Get the drawable's bounds
        val width = bounds.width()
        val height = bounds.height()

        paint.textSize = min(width, height).toFloat() / 2f
        paint.getTextBounds(initials, 0, initials.length, textBounds)

        canvas.drawColor(bgColor)

        canvas.drawText(
            initials,
            (width / 2).toFloat() - textBounds.exactCenterX(),
            (height / 2).toFloat() - textBounds.exactCenterY(),
            paint
        )
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() =
        // Must be PixelFormat.UNKNOWN, TRANSLUCENT, TRANSPARENT, or OPAQUE
        PixelFormat.TRANSLUCENT
}
