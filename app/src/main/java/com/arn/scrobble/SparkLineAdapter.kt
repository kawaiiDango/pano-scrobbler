package com.arn.scrobble

import com.robinhood.spark.SparkAdapter

class SparkLineAdapter(private var yData: List<Int> = emptyList()) : SparkAdapter() {

    var baseline = false

    fun setData(data: List<Int>) {
        yData = data
        notifyDataSetChanged()
    }

    override fun getCount() = yData.size

    override fun getItem(index: Int) = yData[index]

    override fun getY(index: Int) = yData[index].toFloat()

    override fun hasBaseLine() = baseline

    override fun getBaseLine() = 0f

    fun max() = yData.maxOrNull() ?: 0

    fun min() = yData.minOrNull() ?: 0

}