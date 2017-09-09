package com.arn.scrobble

import android.content.Context
import android.net.Uri
import android.preference.Preference
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.*
import android.util.TypedValue
import com.squareup.picasso.Picasso


/**
 * Created by arn on 09/09/2017.
 */

class AppIconsPref : Preference{
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)
    lateinit var packageNames : MutableSet<String>

    override fun onCreateView(parent: ViewGroup): View {
        super.onCreateView(parent)
        val li = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        return li.inflate(R.layout.pref_app_icons, parent, false)
    }

    override fun getView(convertView: View?, parent: ViewGroup): View {
        val v = super.getView(convertView, parent)
        val container = v.findViewById<LinearLayout>(R.id.app_icons_container)
        val title = v.findViewById<TextView>(R.id.app_list_enabled_title)
        title.text = this.title
        val dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, context.resources.displayMetrics).toInt()

        packageNames = sharedPreferences.getStringSet(key, setOf())

        packageNames.forEachIndexed { index, it ->
            if (index > 10)
                return@forEachIndexed
            val app = context.packageManager.getApplicationInfo(it, 0)
            val uri = Uri.parse("android.resource://" + it + "/" + app.icon)
            val icon = ImageView(context)
            icon.scaleType = ImageView.ScaleType.FIT_CENTER
//        icon.layoutParams =
            icon.setPadding(dp/6, dp/6, dp/6, dp/6)
            Picasso.with(context)
                    .load(uri)
                    .resize(dp,dp)
                    .centerInside()
                    .into(icon)
            container.addView(icon)
        }
        return v
    }
}