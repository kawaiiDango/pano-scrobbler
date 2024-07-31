package com.arn.scrobble.pref

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.databinding.ListItemTranslatorBinding
import com.arn.scrobble.ui.GenericDiffCallback

class TranslatorsAdapter : ListAdapter<String, TranslatorsAdapter.TransatorsVH>(
    GenericDiffCallback { o, n -> o == n }
) {
    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransatorsVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemTranslatorBinding.inflate(inflater, parent, false)
        return TransatorsVH(binding)
    }

    override fun onBindViewHolder(holder: TransatorsVH, position: Int) {
        holder.setData(getItem(position))
    }

    class TransatorsVH(private val binding: ListItemTranslatorBinding) :
        RecyclerView.ViewHolder(binding.root) {

//        init {
//            if (!Stuff.isTv) {
//                binding.username.setOnClickListener {
//                    Stuff.openInBrowser(
//                        itemView.context,
//                        "https://crowdin.com/profile/${binding.username.text}"
//                    )
//                }
//            }
//        }

        fun setData(username: String) {
            binding.username.text = username
        }
    }

}