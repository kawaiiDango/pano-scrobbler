package com.arn.scrobble.pref

import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.size.Scale
import com.arn.scrobble.R
import com.arn.scrobble.databinding.HeaderWithActionBinding
import com.arn.scrobble.databinding.ListItemAppBinding
import com.arn.scrobble.ui.ExpandableHeader
import com.arn.scrobble.ui.GenericDiffCallback
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.PackageName
import com.arn.scrobble.ui.SectionedVirtualList
import com.arn.scrobble.utils.Stuff
import com.google.android.material.color.MaterialColors


/**
 * Created by arn on 05/09/2017.
 */
class AppListAdapter(
    context: Context,
    private val viewModel: AppListVM,
    private val singleChoice: Boolean,
) : ListAdapter<Any, RecyclerView.ViewHolder>(
    GenericDiffCallback { o, n ->
        o is ExpandableHeader && n is ExpandableHeader || o === n
    }
), ItemClickListener<Any> {

    private val packageManager = context.packageManager
    private val itemClickListener: ItemClickListener<Any> = this

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            SectionedVirtualList.TYPE_ITEM_DEFAULT -> VHItem(
                ListItemAppBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            SectionedVirtualList.TYPE_HEADER_DEFAULT -> VHHeader(
                HeaderWithActionBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            else -> throw RuntimeException("Invalid view type $viewType")
        }
    }

    override fun getItemViewType(position: Int) = if (getItem(position) is ApplicationInfo)
        SectionedVirtualList.TYPE_ITEM_DEFAULT
    else
        SectionedVirtualList.TYPE_HEADER_DEFAULT

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is VHItem -> holder.setData(getItem(position) as ApplicationInfo)

            is VHHeader -> holder.setData(getItem(position) as ExpandableHeader)

            else -> throw RuntimeException("Invalid view type $holder")
        }
    }

    override fun onItemClick(view: View, position: Int, item: Any) {
        if (item !is ApplicationInfo) return
        val packageName = item.packageName

        if (singleChoice) {
            val previousIndex = currentList.indexOfFirst {
                it is ApplicationInfo && it.packageName in viewModel.selectedPackages
            }

            viewModel.selectedPackages.clear()
            viewModel.selectedPackages += packageName

            notifyItemChanged(previousIndex, 0)
            notifyItemChanged(position, 0)

        } else {
            if (packageName in viewModel.selectedPackages)
                viewModel.selectedPackages -= packageName
            else
                viewModel.selectedPackages += packageName
            notifyItemChanged(position, 0)
        }
    }

    inner class VHItem(private val binding: ListItemAppBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            binding.root.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            itemClickListener.call(itemView, bindingAdapterPosition) {
                getItem(bindingAdapterPosition)
            }
        }

        fun setData(applicationInfo: ApplicationInfo) {
            val packageName = applicationInfo.packageName
            binding.appListName.text =
                if (packageName == Stuff.PACKAGE_PIXEL_NP || packageName == Stuff.PACKAGE_PIXEL_NP_R)
                    itemView.context.getString(R.string.pixel_np)
                else
                    applicationInfo.loadLabel(packageManager)
            binding.appListIcon.load(PackageName(packageName)) {
                scale(Scale.FIT)
            }
            val isChecked = packageName in viewModel.selectedPackages
            itemView.isActivated = isChecked
            binding.appListCheckbox.isChecked = isChecked
        }
    }

    class VHHeader(private val binding: HeaderWithActionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.headerText.setTextColor(
                MaterialColors.getColor(
                    itemView,
                    com.google.android.material.R.attr.colorSecondary
                )
            )
            binding.headerAction.visibility = View.GONE
        }

        fun setData(headerData: ExpandableHeader) {
            binding.headerText.text = headerData.title
            binding.headerText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                ContextCompat.getDrawable(
                    itemView.context,
                    headerData.iconRes
                ), null, null, null
            )
        }
    }
}
