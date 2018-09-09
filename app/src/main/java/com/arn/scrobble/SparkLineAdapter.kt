package com.arn.scrobble

import com.robinhood.spark.SparkAdapter

class SparkLineAdapter(private var yData: ArrayList<Float> = arrayListOf()) : SparkAdapter() {

    fun setData(data: ArrayList<Float>) {
        yData = data
        notifyDataSetChanged()
    }

    override fun getCount() = yData.size

    override fun getItem(index: Int) = yData[index]

    override fun getY(index: Int) = yData[index]

    fun max() = yData.max() ?: 0f
    fun min() = yData.min() ?: 0f
}