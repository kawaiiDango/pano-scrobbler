package android.support.v4.media.app

import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.support.v4.app.NotificationBuilderWithBuilderAccessor
import android.widget.RemoteViews
import android.support.compat.R

/**
 * Created by arn on 29/09/2017.
 */
class MediaStyleMod: NotificationCompat.MediaStyle() {

    override fun makeContentView(builder: NotificationBuilderWithBuilderAccessor?): RemoteViews? {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            val rv = generateContentView()
            val res = Resources.getSystem()
            val attrs = arrayOf(android.R.attr.textColor).toIntArray()

            var sysStyle = res.getIdentifier("TextAppearance.Material.Notification.Title", "style", "android")
            val titleColorHack = mBuilder.mContext.obtainStyledAttributes(sysStyle, attrs).getColor(0, Color.BLACK)

            sysStyle = res.getIdentifier("TextAppearance.Material.Notification", "style", "android")
            val descColorHack = mBuilder.mContext.obtainStyledAttributes(sysStyle, attrs).getColor(0, Color.BLACK)

            rv.setTextColor(R.id.title, titleColorHack)
            rv.setTextColor(R.id.text, descColorHack)
            rv.setTextColor(R.id.text2, descColorHack)

            return rv
        } else
            return null
    }
}