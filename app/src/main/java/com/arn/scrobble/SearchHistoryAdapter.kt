package com.arn.scrobble

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.databinding.ContentSearchBinding
import com.arn.scrobble.databinding.ListItemSearchHistoryBinding
import com.arn.scrobble.ui.ItemClickListener


class SearchHistoryAdapter(private val fragmentBinding: ContentSearchBinding):
        RecyclerView.Adapter<SearchHistoryAdapter.VHSearchHistory>() {
    lateinit var clickListener: ItemClickListener
    lateinit var viewModel: SearchVM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHSearchHistory {
        val inflater = LayoutInflater.from(parent.context)
        return VHSearchHistory(ListItemSearchHistoryBinding.inflate(inflater, parent, false), clickListener)
    }

    override fun onBindViewHolder(holder: VHSearchHistory, position: Int) {
        holder.setData(viewModel.history[viewModel.history.size - position - 1])
    }

    override fun getItemCount() = viewModel.history.size

    fun populate() {
        notifyDataSetChanged()
        fragmentBinding.searchProgress.visibility = View.GONE
        fragmentBinding.searchResultsList.visibility = View.GONE
        fragmentBinding.searchHistoryList.visibility = View.VISIBLE
    }

    class VHSearchHistory(private val binding: ListItemSearchHistoryBinding, private val itemClickListener: ItemClickListener):
            RecyclerView.ViewHolder(binding.root) {

        fun setData(text: String) {
            binding.searchHistoryItem.text = text
            binding.searchHistoryItem.setOnClickListener { itemClickListener.onItemClick(itemView, adapterPosition) }
        }
    }
}