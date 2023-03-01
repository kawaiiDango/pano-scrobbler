package com.arn.scrobble.edits

import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ListItemRegexEditBinding
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.ui.ItemClickListener
import com.google.android.material.color.MaterialColors


class RegexEditsAdapter(
    private val viewModel: RegexEditsVM,
    private val itemClickListener: ItemClickListener,
) : RecyclerView.Adapter<RegexEditsAdapter.VHRegexEdit>() {

    lateinit var itemTouchHelper: RegexItemTouchHelper

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHRegexEdit {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = VHRegexEdit(
            ListItemRegexEditBinding.inflate(inflater, parent, false),
            itemClickListener
        )
        viewHolder.binding.editHandle.setOnTouchListener { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                itemTouchHelper.startDrag(viewHolder)
            }
            true
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: VHRegexEdit, position: Int) {
        holder.setItemData(viewModel.regexes[position])
    }

    fun getItem(idx: Int) = viewModel.regexes[idx]

    override fun getItemCount() = viewModel.regexes.size

    override fun getItemId(position: Int) = viewModel.regexes[position]._id.toLong()

    class VHRegexEdit(
        val binding: ListItemRegexEditBinding,
        private val itemClickListener: ItemClickListener
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener { itemClickListener.call(it, bindingAdapterPosition) }
        }

        fun setItemData(editParam: RegexEdit) {
            val e = RegexPresets.getPossiblePreset(editParam)
            if (e.preset == null) {
                binding.editPattern.text = if (e.name != null)
                    e.name
                else
                    e.pattern
            } else {
                binding.editPattern.text = itemView.context.getString(
                    R.string.edit_preset_name,
                    RegexPresets.getString(e.preset!!)
                )
            }
            if (e.replacement.isEmpty()) {
                binding.editReplacement.visibility = View.GONE
            } else {
                binding.editReplacement.visibility = View.VISIBLE
                binding.editReplacement.text = e.replacement
            }

            val modifiersSet = (e.fields ?: emptySet()).toMutableSet()

            if (e.caseSensitive) {
                modifiersSet.add("Aa")
            }

            if (e.continueMatching) {
                modifiersSet.add(RegexEdit::continueMatching.name)
            }

            if (e.replaceAll) {
                modifiersSet.add("all")
            }

            binding.editModifiers.text = createImageSpans(modifiersSet.joinToString())

            binding.editHandle.setOnClickListener {
                itemClickListener.call(it, bindingAdapterPosition)
            }
            itemView.setOnClickListener {
                itemClickListener.call(it, bindingAdapterPosition)
            }
        }

        private fun createImageSpans(text: String): SpannableString {
            val spannable = SpannableString(text.replace(",", ""))
            val tokens = spannable.split(" ")
            val positions = mutableListOf(-1)
            spannable.forEachIndexed { index, c ->
                if (c == ' ')
                    positions += index
            }
            tokens.forEachIndexed { index, token ->

                val imgRes = when (token) {
                    NLService.B_TRACK -> R.drawable.vd_note
                    NLService.B_ALBUM -> R.drawable.vd_album
                    NLService.B_ALBUM_ARTIST -> R.drawable.vd_album_artist
                    NLService.B_ARTIST -> R.drawable.vd_mic
                    RegexEdit::continueMatching.name -> R.drawable.vd_arrow_right
                    else -> null
                }

                val contentDesc = when (token) {
                    NLService.B_TRACK -> itemView.context.getString(R.string.track)
                    NLService.B_ALBUM -> itemView.context.getString(R.string.album)
                    NLService.B_ALBUM_ARTIST -> itemView.context.getString(R.string.album_artist)
                    NLService.B_ARTIST -> itemView.context.getString(R.string.artist)
                    RegexEdit::continueMatching.name -> itemView.context.getString(R.string.edit_continue)
                    else -> null
                }

                if (imgRes != null) {
                    val start = positions[index] + 1
                    val end = start + token.length

                    spannable.setSpan(
                        ImageSpan(itemView.context, imgRes).apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                contentDescription = contentDesc
                            }
                            drawable.setTint(
                                MaterialColors.getColor(
                                    itemView.context,
                                    com.google.android.material.R.attr.colorPrimary,
                                    null
                                )
                            )
                        },
                        start,
                        end,
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                }
            }
            return spannable
        }
    }
}
