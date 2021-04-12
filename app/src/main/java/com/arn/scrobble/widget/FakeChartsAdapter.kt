package com.arn.scrobble.widget

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.arn.scrobble.R
import com.arn.scrobble.databinding.AppwidgetChartsItemBinding
import java.text.NumberFormat
import android.view.LayoutInflater




class FakeChartsAdapter(
        context: Context,
        private val isDark: Boolean,
        private val hasShadow: Boolean,
    ): ArrayAdapter<ChartsWidgetListItem>(context, R.layout.appwidget_charts_item) {
    private val inflater by lazy { context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater }

    init {
        addAll(
            ChartsWidgetListItem("Nachtansicht", "Alstroemeria Records", 98),
            ChartsWidgetListItem("BLOSSOM", "Studio \"Syrup Comfiture\"", 79),
            ChartsWidgetListItem("swimming classroom.", "Macaroom", 57),
            ChartsWidgetListItem("Identity.", "syrufit", 36),
            ChartsWidgetListItem("Lucky 7", "Halozy", 33),
            ChartsWidgetListItem("Frozen Traveler", "DiGiTAL WiNG", 30),
            ChartsWidgetListItem("Millennium Mother", "Mli", 30),
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

        //takes style attributes from main theme instead of the immediate one
        val textColor = if (isDark)
            Color.WHITE
        else
            Color.BLACK
        val shadowColor = if (!hasShadow)
            Color.TRANSPARENT
        else if (!isDark)
            Color.WHITE
        else
            Color.BLACK
        arrayOf(
            binding.appwidgetChartsSerial,
            binding.appwidgetChartsTitle,
            binding.appwidgetChartsSubtitle,
            binding.appwidgetChartsPlays
        ).forEach {
            it.setTextColor(textColor)
            it.setShadowLayer(6f, 2f, 2f, shadowColor)
        }
        return view
    }
}