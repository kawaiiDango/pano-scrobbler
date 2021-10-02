package com.arn.scrobble.pref

import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.loadAny
import coil.size.Scale
import com.arn.scrobble.R
import com.arn.scrobble.databinding.HeaderDefaultBinding
import com.arn.scrobble.databinding.ListItemAppBinding
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.PackageName
import com.arn.scrobble.ui.VHHeader
import com.google.android.material.color.MaterialColors


/**
 * Created by arn on 05/09/2017.
 */
class AppListAdapter(
    context: Context,
    private val prefsSet: Set<String>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ItemClickListener {

    private val sectionHeaders = mutableMapOf<Int,String>()
    private val packageManager = context.packageManager
    private val appList = mutableListOf<ApplicationInfo?>()
    private var itemClickListener: ItemClickListener = this
    private val selectedItems = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ITEM -> VHItem(ListItemAppBinding.inflate(inflater, parent, false))
            TYPE_HEADER -> VHHeader(HeaderDefaultBinding.inflate(inflater, parent, false))
            else -> throw RuntimeException("Invalid view type $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (sectionHeaders.containsKey(position))
            TYPE_HEADER
        else
            TYPE_ITEM
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is VHItem -> appList[position]?.let {
                holder.setItemData(it)
            }
            is VHHeader -> {
                holder.setHeaderText(sectionHeaders[position] ?: "...")
                holder.setHeaderTextColor(MaterialColors.getColor(holder.itemView, R.attr.colorSecondary))
            }
            else -> throw RuntimeException("Invalid view type $holder")
        }
    }

    fun addSectionHeader(text: String) {
        sectionHeaders[itemCount] = text
        add(null)
    }

    fun add(app: ApplicationInfo?, selected:Boolean = false) {
        if (selected || app?.packageName in prefsSet)
            selectedItems.add(itemCount)
        appList.add(app)
    }

    override fun onItemClick(view: View, position: Int) {
        if(getItemViewType(position) == TYPE_ITEM) {
            if (position in selectedItems)
                selectedItems.remove(position)
            else
                selectedItems.add(position)
            notifyItemChanged(position, PAYLOAD_CLICKED)
        }
    }

    fun getSelectedPackages(): List<String> {
        val list = mutableListOf<String>()
        selectedItems.forEach {
            if (it < itemCount)
                appList[it]?.let { list.add(it.packageName) }
        }
        return list
    }
    override fun getItemCount() = appList.size

    inner class VHItem(private val binding: ListItemAppBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener{
        init {
            binding.root.setOnClickListener(this)
            binding.appListCheckbox.setOnCheckedChangeListener(null)
        }

        override fun onClick(view: View) {
            itemClickListener.call(itemView, bindingAdapterPosition)
        }

        fun setItemData(app: ApplicationInfo) {
            binding.appListName.text = app.loadLabel(packageManager)
            binding.appListIcon.loadAny(PackageName(app.packageName)) {
                scale(Scale.FIT)
            }
            setChecked(true)
        }

        private fun setChecked(animate: Boolean) {
            val isSelected = bindingAdapterPosition in selectedItems
            if (!animate){
                binding.appListCheckbox.setOnCheckedChangeListener(null)
            }
            itemView.isActivated = isSelected
            binding.appListCheckbox.isChecked = isSelected

        }
    }
}

private const val TYPE_ITEM = 0
private const val TYPE_HEADER = 1
private const val PAYLOAD_CLICKED = 6