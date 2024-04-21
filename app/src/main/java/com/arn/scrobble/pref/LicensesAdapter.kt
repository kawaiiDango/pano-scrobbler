package com.arn.scrobble.pref

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.databinding.ListItemLicenseBinding
import com.arn.scrobble.ui.GenericDiffCallback
import com.arn.scrobble.utils.Stuff
import com.mikepenz.aboutlibraries.entity.Library

class LicensesAdapter : ListAdapter<Library, LicensesAdapter.LicensesVH>(
    GenericDiffCallback { o, n -> o.uniqueId == n.uniqueId }
) {
    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LicensesVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemLicenseBinding.inflate(inflater, parent, false)
        return LicensesVH(binding)
    }

    override fun onBindViewHolder(holder: LicensesVH, position: Int) {
        holder.setData(getItem(position))
    }

    class LicensesVH(private val binding: ListItemLicenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var url: String? = null

        init {
            if (!Stuff.isTv) {
                binding.root.setOnClickListener {
                    if (url.isNullOrEmpty())
                        return@setOnClickListener
                    Stuff.openInBrowser(url!!)
                }
            }
        }

        fun setData(library: Library) {
            var name = if (library.name == "\${project.artifactId}") // is this a library bug?
                library.uniqueId.split(':').last()
            else
                library.name

            name += " " + library.artifactVersion

            binding.libraryName.text = name
            binding.libraryUniqueId.text = library.uniqueId
            binding.libraryLicense.text = library.licenses.joinToString { it.name }

            url = library.website ?: library.scm?.url ?: library.licenses.firstOrNull()?.url
        }
    }


}