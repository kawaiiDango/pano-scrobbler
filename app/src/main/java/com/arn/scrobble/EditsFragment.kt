package com.arn.scrobble

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.content_edits.view.*

/**
 * Created by arn on 21/09/2017.
 */
class EditsFragment: Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.content_edits, container, false)
        val adapter = EditsAdapter(context!!)
        view.edits_list.layoutManager = LinearLayoutManager(context!!)
        view.edits_list.adapter = adapter
        adapter.loadAll()
        return view
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.edits)
    }
}