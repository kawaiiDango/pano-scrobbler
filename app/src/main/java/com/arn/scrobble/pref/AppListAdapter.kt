package com.arn.scrobble.pref

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.preference.PreferenceManager
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.squareup.picasso.Picasso


/**
 * Created by arn on 05/09/2017.
 */
class AppListAdapter
(c: Context, private val itemResourceId:Int, private val headerResourceId:Int) : ArrayAdapter<ApplicationInfo>(c, itemResourceId) {
    private val sectionHeaders = mutableMapOf<Int,String>()
    private val prefsSet = PreferenceManager.getDefaultSharedPreferences(context).getStringSet(Stuff.APP_WHITELIST, setOf())
    private var list = (c as Activity).findViewById<ListView>(R.id.app_list)
    private var picasso: Picasso = Picasso.Builder(context)
            .addRequestHandler(AppIconRequestHandler(context))
            .build()
    private val dp = Stuff.dp2px(48, context)
    private var lastCheckedPos = 0

    override fun getView(position: Int, convertView: View?, list: ViewGroup): View {
        var convertView : View? = convertView
        val type = getItemViewType(position)
        list as ListView

        if (convertView == null) {
            // inflate the layout
            val inflater = (context as Activity).layoutInflater
                convertView = inflater.inflate(
                        if(type == TYPE_ITEM) itemResourceId else headerResourceId
                        , list, false)!!
        }
        if (type == TYPE_ITEM) {
            val app = getItem(position) ?: return convertView
            val checkbox = convertView.findViewById<CheckBox>(R.id.app_list_checkbox)
            val tv = convertView.findViewById<TextView>(R.id.app_list_name)
            val icon = convertView.findViewById<ImageView>(R.id.app_list_icon)

            tv.text = app.loadLabel(context.packageManager) ?: return convertView
            tv.tag = app.packageName
            val uri = Uri.parse(AppIconRequestHandler.SCHEME_PNAME  +":" + app.packageName)

            picasso.load(uri)
//                    .placeholder(android.R.color.transparent)
                    .resize(dp, dp)
                    .into(icon)

            checkbox.isChecked = list.isItemChecked(position)
            checkbox.setOnClickListener { cb ->
                list.setItemChecked(position, (cb as CheckBox).isChecked)
            }
        } else {
            convertView.findViewById<TextView>(R.id.header_text).text = sectionHeaders[position]

        }
        return convertView
    }

    fun addSectionHeader(text: String) {
        sectionHeaders[count] = text
        add(ApplicationInfo())
        lastCheckedPos = count
    }

    override fun add(app: ApplicationInfo?) {
        if (prefsSet.contains(app?.packageName)) {
            super.insert(app, lastCheckedPos)
            list?.setItemChecked(lastCheckedPos, true)
            lastCheckedPos++
        } else
            super.add(app)
    }
    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItemViewType(position: Int): Int {

        return if (getItem(position).flags != 0) {
            TYPE_ITEM
        } else TYPE_HEADER
    }

    override fun isEnabled(pos: Int): Boolean {
        return getItemViewType(pos) == TYPE_ITEM
    }

    override fun hasStableIds(): Boolean {
        return true
    }
    companion object {
        private val TYPE_ITEM = 0
        private val TYPE_HEADER = 1
    }
}
