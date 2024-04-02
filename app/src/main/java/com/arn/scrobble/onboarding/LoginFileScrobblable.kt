package com.arn.scrobble.onboarding

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.R
import com.arn.scrobble.api.file.FileScrobblable
import com.arn.scrobble.databinding.ContentLoginFileScrobblableBinding
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.transition.MaterialSharedAxis

class LoginFileScrobblable : Fragment() {
    private var _binding: ContentLoginFileScrobblableBinding? = null
    private val binding get() = _binding!!
    private lateinit var fileScrobblableCreate: ActivityResultLauncher<String>
    private lateinit var fileScrobblableOpen: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fileScrobblableCreate =
            registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
                uri ?: return@registerForActivityResult
                onFilePicked(uri)
            }

        fileScrobblableOpen =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri ?: return@registerForActivityResult
                onFilePicked(uri)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentLoginFileScrobblableBinding.inflate(inflater, container, false)
        setupAxisTransitions(MaterialSharedAxis.X)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = ContentLoginFileScrobblableBinding.bind(view)

        binding.loginCreate.setOnClickListener {
            val extension = when (binding.loginFileFormat.checkedChipId) {
                R.id.login_file_format_csv -> ".csv"
                R.id.login_file_format_jsonl -> ".jsonl"
                else -> throw IllegalStateException("Unknown file format")
            }

            kotlin.runCatching {
                fileScrobblableCreate.launch("scrobbles_log_" + Stuff.getFileNameDateSuffix() + extension)
            }
        }

        binding.loginOpen.setOnClickListener {
            val mimeTypes = when (binding.loginFileFormat.checkedChipId) {
                R.id.login_file_format_csv -> arrayOf("text/comma-separated-values", "text/csv")
                R.id.login_file_format_jsonl -> arrayOf("*/*")

                else -> throw IllegalStateException("Unknown file format")
            }

            kotlin.runCatching {
                fileScrobblableOpen.launch(mimeTypes)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onFilePicked(uri: Uri) {
        val format = when (binding.loginFileFormat.checkedChipId) {
            R.id.login_file_format_csv -> FileScrobblable.FileFormat.csv
            R.id.login_file_format_jsonl -> FileScrobblable.FileFormat.jsonl
            else -> throw IllegalStateException("Unknown file format")
        }
        FileScrobblable.authAndGetSession(uri, format)
            .onSuccess { findNavController().popBackStack() }
            .onFailure { requireContext().toast(it.message.toString()) }
    }

}