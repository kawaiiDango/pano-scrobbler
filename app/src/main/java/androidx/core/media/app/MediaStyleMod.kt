package androidx.core.media.app

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.core.R
import androidx.core.app.NotificationBuilderWithBuilderAccessor
import androidx.media.app.NotificationCompat

/**
 * Created by arn on 29/09/2017.
 */
class MediaStyleMod : NotificationCompat.MediaStyle() {
    @SuppressLint("RestrictedApi")
    override fun makeContentView(builder: NotificationBuilderWithBuilderAccessor?): RemoteViews? {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            //Accessing @hide methods is forbidden only for P+

            val generateContentView =
                NotificationCompat.MediaStyle::class.java.getDeclaredMethod("generateContentView")
            generateContentView.isAccessible = true
            val rv = generateContentView.invoke(this) as RemoteViews
//            val rv = generateContentView()
            val res = Resources.getSystem()
            val attrs = arrayOf(android.R.attr.textColor).toIntArray()

            var sysStyle =
                res.getIdentifier("TextAppearance.Material.Notification.Title", "style", "android")
            var ta = mBuilder.mContext.obtainStyledAttributes(sysStyle, attrs)
            val titleColorHack = ta.getColor(0, Color.GRAY)
            ta.recycle()

            sysStyle = res.getIdentifier("TextAppearance.Material.Notification", "style", "android")
            ta = mBuilder.mContext.obtainStyledAttributes(sysStyle, attrs)
            val descColorHack = ta.getColor(0, Color.GRAY)
            ta.recycle()

            rv.setTextColor(R.id.title, titleColorHack)
            rv.setTextColor(R.id.text, descColorHack)
            rv.setTextColor(R.id.text2, descColorHack)

            return rv
        } else
            return null
    }
}