package com.arn.scrobble.ui

import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.databinding.HeaderDefaultBinding

class VHHeader(private val binding: HeaderDefaultBinding) : RecyclerView.ViewHolder(binding.root) {
    fun setHeaderText(s: String) {
        binding.headerText.text = s
    }

    fun setHeaderTextColor(color: Int) {
        binding.headerText.setTextColor(color)
    }
}