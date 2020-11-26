package com.arn.scrobble.pref

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.HeaderDefaultBinding
import com.arn.scrobble.databinding.ListItemAppBinding
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.VHHeader
import com.squareup.picasso.Picasso


/**
 * Created by arn on 05/09/2017.
 */
class AppListAdapter
(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ItemClickListener {

    private val sectionHeaders = mutableMapOf<Int,String>()
    private val packageManager = context.packageManager
    private val appList = mutableListOf<ApplicationInfo?>()
    private var itemClickListener: ItemClickListener = this
    private val selectedItems = mutableSetOf<Int>()
    private val prefsSet = MultiPreferences(context).getStringSet(Stuff.PREF_WHITELIST, setOf())

    private var picasso: Picasso = Picasso.Builder(context)
            .addRequestHandler(AppIconRequestHandler(context))
            .build()

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
                holder.setHeaderTextColor(ContextCompat.getColor(holder.itemView.context, R.color.colorAccent))
            }
            else -> throw RuntimeException("Invalid view type $holder")
        }
    }

    fun addSectionHeader(text: String) {
        sectionHeaders[itemCount] = text
        add(null)
    }

    fun add(app: ApplicationInfo?, selected:Boolean = false) {
        if (selected || prefsSet.contains(app?.packageName))
            selectedItems.add(itemCount)
        appList.add(app)
    }

    override fun onItemClick(view: View, position: Int) {
        if(getItemViewType(position) == TYPE_ITEM) {
            if (selectedItems.contains(position))
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
            itemClickListener.onItemClick(itemView, adapterPosition)
        }

        fun setItemData(app: ApplicationInfo) {
            binding.appListName.text = app.loadLabel(packageManager)
            val uri = Uri.parse(AppIconRequestHandler.SCHEME_PNAME + ":" + app.packageName)

            picasso.load(uri)
                    .fit()
                    .into(binding.appListIcon)
            setChecked(true)
        }

        private fun setChecked(animate: Boolean) {
            val isSelected = selectedItems.contains(adapterPosition)
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