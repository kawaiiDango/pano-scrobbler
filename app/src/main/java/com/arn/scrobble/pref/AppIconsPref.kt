package com.arn.scrobble.pref

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.pref_app_icons.view.*


/**
 * Created by arn on 09/09/2017.
 */

class AppIconsPref : Preference {
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle){
        layoutResource = R.layout.pref_app_icons
//        widgetLayoutResource = R.layout.pref_app_icons_widget
    }
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)
    constructor(context: Context) : super(context)

    private val picasso by lazy {
        Picasso.Builder(context)
                .addRequestHandler(AppIconRequestHandler(context))
                .build()!!
    }
    private val wPx by lazy { Stuff.dp2px(48, context) }


//    override fun onCreateView(parent: ViewGroup): View {
//        super.onCreateView(parent)
//        val li = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//        return li.inflate(R.layout.pref_app_icons, parent, false)
//    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.isDividerAllowedAbove = false
        val v = holder.itemView
        val packageNames = preferenceDataStore!!.getStringSet(key, setOf())!!

        if (packageNames.isNotEmpty()) {
            v.app_icons_container.removeAllViews()

            val scrW = context.resources.displayMetrics.widthPixels

            val nIcons = (scrW / (wPx * 1.16)).toInt() - 1
            for (i in 0 until minOf(nIcons, packageNames.count())) {
                val icon = ImageView(context)
                icon.scaleType = ImageView.ScaleType.FIT_CENTER
                icon.setPadding(wPx / 6, 0, wPx / 6, wPx / 6)
                val uri = Uri.parse(AppIconRequestHandler.SCHEME_PNAME + ":" + packageNames.elementAt(i))
                picasso.load(uri)
                        .resize(wPx, wPx)
                        .into(icon)
                v.app_icons_container.addView(icon)
            }

            if (packageNames.size > nIcons) {
                v.app_list_summary.visibility = View.VISIBLE
                v.app_list_summary.text = context.getString(R.string.n_more, packageNames.size - nIcons)
            } else
                v.app_list_summary.visibility = View.GONE
        } else {
            v.app_list_summary.visibility = View.VISIBLE
            v.app_list_summary.text = context.getString(R.string.no_apps_enabled)
        }
        if (isIconSpaceReserved)
            v.setPaddingRelative(Stuff.dp2px(48, context), 0, 0, 0)
    }
}