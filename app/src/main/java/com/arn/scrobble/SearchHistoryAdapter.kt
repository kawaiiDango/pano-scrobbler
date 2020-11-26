package com.arn.scrobble

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.ui.ItemClickListener
import kotlinx.android.synthetic.main.content_search.view.*
import kotlinx.android.synthetic.main.list_item_search_history.view.*


class SearchHistoryAdapter(private val fragmentContent: View):
        RecyclerView.Adapter<SearchHistoryAdapter.VHSearchHistory>() {
    lateinit var clickListener: ItemClickListener
    lateinit var viewModel: SearchVM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHSearchHistory {
        val inflater = LayoutInflater.from(parent.context)
        return VHSearchHistory(inflater.inflate(R.layout.list_item_search_history, parent, false), clickListener)
    }

    override fun onBindViewHolder(holder: VHSearchHistory, position: Int) {
        holder.setData(viewModel.history[viewModel.history.size - position - 1])
    }

    override fun getItemCount() = viewModel.history.size

    fun populate() {
        notifyDataSetChanged()
        fragmentContent.search_progress.visibility = View.GONE
        fragmentContent.search_results_list.visibility = View.GONE
        fragmentContent.search_history_list.visibility = View.VISIBLE
    }

    class VHSearchHistory(view: View, private val itemClickListener: ItemClickListener): RecyclerView.ViewHolder(view) {
        private val vText = view.search_history_item

        fun setData(text: String) {
            vText.text = text
            vText.setOnClickListener { itemClickListener.onItemClick(itemView, adapterPosition) }
        }
    }
}