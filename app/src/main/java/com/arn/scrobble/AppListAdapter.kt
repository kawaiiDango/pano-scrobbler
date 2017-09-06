package com.arn.scrobble

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.squareup.picasso.Picasso


/**
 * Created by arn on 05/09/2017.
 */
class AppListAdapter
(private val c: Context, private val itemResourceId:Int, private val headerResourceId:Int) : ArrayAdapter<ApplicationInfo>(c, itemResourceId) {
    private val sectionHeaders = mutableMapOf<Int,String>()
    private val prefsSet = c.getSharedPreferences(Stuff.APP_LIST_PREFS, Context.MODE_PRIVATE).getStringSet(Stuff.APP_LIST_PREFS, setOf())
    var list:ListView? = null
    init{
        list = (c as Activity).findViewById(R.id.app_list)
    }

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

            tv.text = app.loadLabel(c.packageManager) ?: return convertView
            tv.tag = app.packageName
            val uri = Uri.parse("android.resource://" + app.packageName + "/" + app.icon)
            Picasso.with(context)
                    .load(uri)
                    .placeholder(R.drawable.ic_transparent)
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
    }

    override fun add(app: ApplicationInfo?) {
        super.add(app)
        if (prefsSet.contains(app?.packageName)) {
            list?.setItemChecked(count - 1, true)
        }
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
