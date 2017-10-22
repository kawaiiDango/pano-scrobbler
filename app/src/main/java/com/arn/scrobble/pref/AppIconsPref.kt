package com.arn.scrobble.pref

import android.content.Context
import android.net.Uri
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.ImageView
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.pref_app_icons.view.*


/**
 * Created by arn on 09/09/2017.
 */

class AppIconsPref : Preference{
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)
    private lateinit var packageNames : MutableSet<String>
    private var picasso: Picasso = Picasso.Builder(context)
            .addRequestHandler(AppIconRequestHandler(context))
            .build()
    private val dp = Stuff.dp2px(48, context)

    override fun onCreateView(parent: ViewGroup): View {
        super.onCreateView(parent)
        val li = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        return li.inflate(R.layout.pref_app_icons, parent, false)
    }

    override fun getView(convertView: View?, parent: ViewGroup): View {
        val v = super.getView(convertView, parent)

        packageNames = sharedPreferences.getStringSet(key, setOf())

        if (packageNames.count() == 0){
            v.app_list_enabled_title.text = context.getString(R.string.no_apps_enabled)
        } else {
            v.app_list_enabled_title.text = this.title
            for (i in 0 until minOf(10, packageNames.count())) {
                val icon = ImageView(context)
                icon.scaleType = ImageView.ScaleType.FIT_CENTER
                icon.setPadding(dp / 6, dp / 6, dp / 6, dp / 6)
                val uri = Uri.parse(AppIconRequestHandler.SCHEME_PNAME + ":" + packageNames.elementAt(i))
                picasso.load(uri)
                        .resize(dp, dp)
                        .into(icon)
                v.app_icons_container.addView(icon)
            }
        }
        return v
    }
}