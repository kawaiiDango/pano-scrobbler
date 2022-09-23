package com.arn.scrobble.pref

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ListItemLicenseBinding
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.util.withContext

class LicensesAdapter(context: Context) : RecyclerView.Adapter<LicensesAdapter.LicensesVH>() {

    private val libraries by lazy { Libs.Builder().withContext(context).build().libraries }

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LicensesVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemLicenseBinding.inflate(inflater, parent, false)
        return LicensesVH(binding)
    }

    override fun onBindViewHolder(holder: LicensesVH, position: Int) {
        holder.setData(libraries[position])
    }

    override fun getItemCount() = libraries.size

    class LicensesVH(private val binding: ListItemLicenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var url: String? = null

        init {
            binding.root.setOnClickListener {
                if (url.isNullOrEmpty())
                    return@setOnClickListener
                Stuff.openInBrowser(url!!)
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