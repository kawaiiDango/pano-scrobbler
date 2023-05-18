package com.arn.scrobble.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.AppwidgetChartsItemBinding
import java.text.NumberFormat


class FakeChartsAdapter(
    context: Context,
) : ArrayAdapter<ChartsWidgetListItem>(context, R.layout.appwidget_charts_item) {
    private val inflater by lazy { ContextCompat.getSystemService(context, LayoutInflater::class.java)!! }

    init {
        addAll(
            ChartsWidgetListItem("Nachtansicht", "Alstroemeria Records", 98, stonksDelta = -1),
            ChartsWidgetListItem("BLOSSOM", "Studio \"Syrup Comfiture\"", 79, stonksDelta = 10),
            ChartsWidgetListItem("swimming classroom.", "Macaroom", 57, stonksDelta = 0),
            ChartsWidgetListItem("Identity.", "syrufit", 36, stonksDelta = Integer.MAX_VALUE),
            ChartsWidgetListItem("Lucky 7", "Halozy", 33, stonksDelta = 0),
            ChartsWidgetListItem("Frozen Traveler", "DiGiTAL WiNG", 30, stonksDelta = 1),
            ChartsWidgetListItem("Millennium Mother", "Mili", 30, stonksDelta = -11),
        )
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.appwidget_charts_item, parent, false)
        val binding = AppwidgetChartsItemBinding.bind(view)
        val item = getItem(position)!!
        binding.appwidgetChartsSerial.text = NumberFormat.getInstance().format(position + 1) + "."
        binding.appwidgetChartsTitle.text = item.title
        binding.appwidgetChartsSubtitle.text = item.subtitle
        binding.appwidgetChartsPlays.text = NumberFormat.getInstance().format(item.number)
        binding.appwidgetChartsStonksIcon.setImageResource(Stuff.stonksIconForDelta(item.stonksDelta))
        binding.appwidgetChartsStonksIconShadow.setImageResource(Stuff.stonksIconForDelta(item.stonksDelta))

        return view
    }
}