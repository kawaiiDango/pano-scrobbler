package com.arn.scrobble.onboarding

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.R
import com.arn.scrobble.api.file.FileScrobblable
import com.arn.scrobble.databinding.ContentLoginFileScrobblableBinding
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.showWithIcons
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.transition.MaterialSharedAxis

class LoginFileScrobblable : Fragment() {
    private var _binding: ContentLoginFileScrobblableBinding? = null
    private val binding get() = _binding!!
    private lateinit var fileScrobblableCreate: ActivityResultLauncher<String>
    private lateinit var fileScrobblableOpen: ActivityResultLauncher<Array<String>>
    private var fileFormat: FileScrobblable.FileFormat = FileScrobblable.FileFormat.csv

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
            showFileTypeChooser(it, create = true)
        }

        binding.loginOpen.setOnClickListener {
            showFileTypeChooser(it, create = false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showFileTypeChooser(anchor: View, create: Boolean) {
        PopupMenu(requireContext(), anchor).apply {
            menuInflater.inflate(R.menu.file_format_menu, menu)
            setOnMenuItemClickListener {
                runCatching {
                    when (it.itemId) {
                        R.id.file_csv -> {
                            fileFormat = FileScrobblable.FileFormat.csv

                            if (create) {
                                fileScrobblableCreate.launch("scrobbles_log_" + Stuff.getFileNameDateSuffix() + ".csv")
                            } else {
                                fileScrobblableOpen.launch(arrayOf("*/*"))
                                // "text/comma-separated-values" did not work on oreo
                            }
                        }

                        R.id.file_jsonl -> {
                            fileFormat = FileScrobblable.FileFormat.jsonl

                            if (create) {
                                fileScrobblableCreate.launch("scrobbles_log_" + Stuff.getFileNameDateSuffix() + ".jsonl")
                            } else {
                                fileScrobblableOpen.launch(arrayOf("*/*"))
                            }
                        }
                    }
                }.onFailure {
                    requireContext().toast(it.message.toString())
                }
                true
            }
            showWithIcons()
        }
    }

    private fun onFilePicked(uri: Uri) {
        FileScrobblable.authAndGetSession(uri, fileFormat)
            .onSuccess { findNavController().popBackStack() }
            .onFailure { requireContext().toast(it.message.toString()) }
    }

}