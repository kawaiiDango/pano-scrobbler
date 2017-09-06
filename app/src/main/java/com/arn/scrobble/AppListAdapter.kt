package com.arn.scrobble

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.View
import android.view.ViewGroup
import android.widget.*


/**
 * Created by arn on 05/09/2017.
 */
class AppListAdapter
(private val c: Context, private val itemResourceId:Int, private val headerResourceId:Int) : ArrayAdapter<ApplicationInfo>(c, itemResourceId) {
    val prefs = c.getSharedPreferences(Stuff.APP_LIST_PREFS, Context.MODE_PRIVATE)
    val prefsSet = prefs.getStringSet(Stuff.APP_LIST_PREFS, mutableSetOf())
    val sectionHeaders = mutableMapOf<Int,String>()
    private fun updatePrefs(){
        prefs.edit().putStringSet(Stuff.APP_LIST_PREFS, prefsSet).apply()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView : View? = convertView
        val type = getItemViewType(position)

        if (convertView == null) {
            // inflate the layout
            val inflater = (context as Activity).layoutInflater
                convertView = inflater.inflate(
                        if(type == TYPE_ITEM) itemResourceId else headerResourceId
                        , parent, false)!!
        }
        if (type == TYPE_ITEM) {
            val app = getItem(position) ?: return convertView
            val checkbox = convertView.findViewById<CheckBox>(R.id.app_list_checkbox)
            val icon = convertView.findViewById<ImageView>(R.id.app_list_icon)

            checkbox.text = app.loadLabel(c.packageManager) ?: return convertView
            checkbox.tag = app.packageName

            icon.setImageDrawable(app.loadIcon(c.packageManager))
            checkbox.isChecked = prefsSet.contains(app.packageName)
            checkbox.setOnCheckedChangeListener { btn, isChecked ->
                if (isChecked)
                    prefsSet.add(btn.tag as String)
                else
                    prefsSet.remove(btn.tag as String)
                updatePrefs()
                //TODO: move updateprefs to sth like onpause or onstop
                //TODO: make a list bg selector
                parent as ListView
                parent.setItemChecked(position, isChecked)
            }
        } else {
            convertView.findViewById<TextView>(R.id.header_text).text = sectionHeaders[position]

        }
        return convertView
    }

    fun addSectionHeader(text: String) {
        sectionHeaders[count] = text
        add(ApplicationInfo())
//        notifyDataSetChanged()
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItemViewType(position: Int): Int {

        return if (getItem(position).flags != 0) {
            TYPE_ITEM
        } else TYPE_HEADER


    }
    companion object {
        private val TYPE_ITEM = 0
        private val TYPE_HEADER = 1
    }
}
