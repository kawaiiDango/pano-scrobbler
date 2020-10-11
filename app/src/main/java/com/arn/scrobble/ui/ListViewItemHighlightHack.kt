package com.arn.scrobble.ui

import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import com.arn.scrobble.Main
import com.arn.scrobble.R

class ListViewItemHighlightTvHack: AdapterView.OnItemSelectedListener {
    private var prevView: View? = null
    override fun onItemSelected(av: AdapterView<*>?, v: View?, pos: Int, id: Long) {
        if (Main.isTV) {
            v ?: return
            if (v != prevView) {
                v.background = ColorDrawable(ContextCompat.getColor(v.context, R.color.highlightPink))
                prevView?.background = null
            }
            prevView = v
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }
}