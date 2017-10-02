package android.support.v4.media.app

import android.graphics.Color
import android.support.v4.app.NotificationBuilderWithBuilderAccessor
import android.widget.RemoteViews
import android.support.compat.R
import com.arn.scrobble.Stuff

/**
 * Created by arn on 29/09/2017.
 */
class LegacyNotiStyle: NotificationCompat.MediaStyle() {

    override fun makeContentView(builder: NotificationBuilderWithBuilderAccessor?): RemoteViews? {
//        val rv =  generateContentView()
//        return rv
        return null
    }
}