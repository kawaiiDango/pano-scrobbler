package com.arn.scrobble.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.arn.scrobble.R
import com.arn.scrobble.pref.WidgetPrefs
import com.arn.scrobble.utils.Stuff.format


class FakeChartsAdapter(
    context: Context,
) : ArrayAdapter<WidgetPrefs.ChartsWidgetListItem>(context, R.layout.appwidget_charts_item) {
    private val inflater by lazy {
        context.getSystemService(LayoutInflater::class.java)!!
    }

    private var showImages = true

    init {
        addAll(
            WidgetPrefs.ChartsWidgetListItem(
                "Nachtansicht",
                "Alstroemeria Records",
                98,
                stonksDelta = -1
            ),
            WidgetPrefs.ChartsWidgetListItem(
                "BLOSSOM",
                "Studio \"Syrup Comfiture\"",
                79,
                stonksDelta = 10
            ),
            WidgetPrefs.ChartsWidgetListItem(
                "swimming classroom.",
                "Macaroom",
                57,
                stonksDelta = 0
            ),
            WidgetPrefs.ChartsWidgetListItem(
                "Identity.",
                "syrufit",
                36,
                stonksDelta = Integer.MAX_VALUE
            ),
            WidgetPrefs.ChartsWidgetListItem("Lucky 7", "Halozy", 33, stonksDelta = 0),
            WidgetPrefs.ChartsWidgetListItem(
                "Frozen Traveler",
                "DiGiTAL WiNG",
                30,
                stonksDelta = 1
            ),
            WidgetPrefs.ChartsWidgetListItem("Millennium Mother", "Mili", 30, stonksDelta = -11),
        )
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.appwidget_charts_item, parent, false)
        val item = getItem(position)!!
        view.findViewById<TextView>(R.id.appwidget_charts_serial).text = (position + 1).format()
        view.findViewById<TextView>(R.id.appwidget_charts_title).text = item.title
        view.findViewById<TextView>(R.id.appwidget_charts_subtitle).text = item.subtitle
        view.findViewById<TextView>(R.id.appwidget_charts_plays).text = item.number.format()
//        view.findViewById<ImageView>(R.id.appwidget_charts_stonks_icon)
//            .setImageResource(ChartsListUtils.stonksIconForDelta(item.stonksDelta))
//        view.findViewById<ImageView>(R.id.appwidget_charts_stonks_icon_shadow)
//            .setImageResource(ChartsListUtils.stonksIconForDelta(item.stonksDelta))
        view.findViewById<ImageView>(R.id.appwidget_charts_image)
            .setImageResource(R.drawable.vd_album)

        if (showImages) {
            view.findViewById<TextView>(R.id.appwidget_charts_serial).visibility = View.GONE
            view.findViewById<ImageView>(R.id.appwidget_charts_image).visibility = View.VISIBLE

        } else {
            view.findViewById<TextView>(R.id.appwidget_charts_serial).visibility = View.VISIBLE
            view.findViewById<ImageView>(R.id.appwidget_charts_image).visibility = View.GONE
        }

        return view
    }

    fun setShowImages(show: Boolean) {
        if (show == showImages) return
        showImages = show
        notifyDataSetChanged()
    }
}