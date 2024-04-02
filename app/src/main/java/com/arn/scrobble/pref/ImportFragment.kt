package com.arn.scrobble.pref

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ContentImexportBinding
import com.arn.scrobble.databinding.DialogImportBinding
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class ImportFragment : Fragment() {
    private var _binding: ContentImexportBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel by viewModels<ImportVM>()
    private lateinit var importRequest: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAxisTransitions(MaterialSharedAxis.X, MaterialSharedAxis.X)

        importRequest =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri ?: return@registerForActivityResult

                import {
                    requireContext().contentResolver.openInputStream(uri)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentImexportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.methodNetwork.setOnClickListener {
            binding.code.isVisible = true

            viewModel.startServer()
        }

        binding.methodFile.setOnClickListener {
            binding.code.isVisible = false

            // On Android 11 TV:
            // Permission Denial: opening provider com.android.externalstorage.ExternalStorageProvider
            // from ProcessRecord{a608cee 5039:com.google.android.documentsui/u0a21}
            // (pid=5039, uid=10021) requires that you obtain access using ACTION_OPEN_DOCUMENT or related APIs
            importRequest.launch(arrayOf(Stuff.MIME_TYPE_JSON))
        }

        binding.codeEdittext.inputType = InputType.TYPE_NULL
        binding.code.endIconMode = TextInputLayout.END_ICON_NONE

        if (Stuff.isTv) {
            binding.methodFile.isVisible = false
            binding.methodNetwork.callOnClick()
        }

        collectLatestLifecycleFlow(viewModel.postData.filterNotNull()) { postData ->
            import { postData.byteInputStream() }
        }

        collectLatestLifecycleFlow(viewModel.serverAddress.filterNotNull()) { result ->
            result.onSuccess {
                binding.codeEdittext.setText(it)
            }.onFailure {
                binding.code.error = it.message
            }
        }

    }


    private fun import(
        inputStream: () -> InputStream?
    ) {
        val binding = DialogImportBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setTitle(R.string.import_options)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val editsModeMap = mapOf(
                    R.id.import_edits_nope to EditsMode.EDITS_NOPE,
                    R.id.import_edits_replace_all to EditsMode.EDITS_REPLACE_ALL,
                    R.id.import_edits_replace_existing to EditsMode.EDITS_REPLACE_EXISTING,
                    R.id.import_edits_keep to EditsMode.EDITS_KEEP_EXISTING
                )
                val editsMode = editsModeMap[binding.importRadioGroup.checkedRadioButtonId]!!
                val settingsMode = binding.importSettings.isChecked
                if (editsMode == EditsMode.EDITS_NOPE && !settingsMode)
                    return@setPositiveButton
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val imported = inputStream()?.use {
                        viewModel.imExporter.import(it, editsMode, settingsMode)
                    } ?: false

                    withContext(Dispatchers.Main) {
                        if (!imported)
                            requireContext().toast(R.string.import_hey_wtf, Toast.LENGTH_LONG)
                        else {
                            requireContext().toast(R.string.imported)
                            findNavController().popBackStack()
//                            parentFragmentManager
//                                .beginTransaction()
//                                .detach(this@ImportFragment)
//                                .commit()
//                            parentFragmentManager
//                                .beginTransaction()
//                                .attach(this@ImportFragment)
//                                .commit()
                        }
                    }
                }
            }
            .show()
    }
}