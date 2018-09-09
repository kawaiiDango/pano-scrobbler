package androidx.core.media.app

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
class MediaStyleMod: NotificationCompat.MediaStyle() {
    override fun makeContentView(builder: NotificationBuilderWithBuilderAccessor?): RemoteViews? {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            //Accessing @hide methods is forbidden only for P+

            val generateContentView = NotificationCompat.MediaStyle::class.java.getDeclaredMethod("generateContentView")
            if (generateContentView != null) {
                generateContentView.isAccessible = true
                val rv = generateContentView.invoke(this) as RemoteViews
//            val rv = generateContentView()
                val res = Resources.getSystem()
                val attrs = arrayOf(android.R.attr.textColor).toIntArray()

                var sysStyle = res.getIdentifier("TextAppearance.Material.Notification.Title", "style", "android")
                val titleColorHack = mBuilder.mContext.obtainStyledAttributes(sysStyle, attrs).getColor(0, Color.GRAY)

                sysStyle = res.getIdentifier("TextAppearance.Material.Notification", "style", "android")
                val descColorHack = mBuilder.mContext.obtainStyledAttributes(sysStyle, attrs).getColor(0, Color.GRAY)

                rv.setTextColor(R.id.title, titleColorHack)
                rv.setTextColor(R.id.text, descColorHack)
                rv.setTextColor(R.id.text2, descColorHack)

                return rv
            }
            return null
        } else
            return null
    }
}