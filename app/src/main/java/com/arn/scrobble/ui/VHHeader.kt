package com.arn.scrobble.ui

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.header_default.view.*

class VHHeader(view: View) : RecyclerView.ViewHolder(view) {
    private val vText = view.header_text

    fun setHeaderText(s:String) {
        vText.text = s
    }

    fun setHeaderTextColor(color:Int){
        vText.setTextColor(color)
    }
}