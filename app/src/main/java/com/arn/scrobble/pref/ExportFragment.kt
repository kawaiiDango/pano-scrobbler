package com.arn.scrobble.pref

import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.R
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.databinding.ContentImexportBinding
import com.arn.scrobble.main.App
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ExportFragment : Fragment() {
    private var _binding: ContentImexportBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel by viewModels<ExportVM>()
    private val billingViewModel by viewModels<BillingViewModel>()
    private lateinit var exportRequest: ActivityResultLauncher<String>
    private lateinit var exportPrivateDataRequest: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exportRequest =
            registerForActivityResult(ActivityResultContracts.CreateDocument(Stuff.MIME_TYPE_JSON)) { uri ->
                export(uri)
            }

        exportPrivateDataRequest =
            registerForActivityResult(ActivityResultContracts.CreateDocument(Stuff.MIME_TYPE_JSON)) { uri ->
                export(uri, privateData = true)
            }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.X, MaterialSharedAxis.X)

        _binding = ContentImexportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.code.setEndIconOnClickListener {
            viewModel.export(binding.codeEdittext.text.toString())
        }

        binding.codeEdittext.inputType =
            EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_CAP_CHARACTERS

        binding.codeEdittext.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN)
            ) {
                viewModel.export(textView.text.toString().uppercase())
                true
            } else
                false
        }

        binding.codeEdittext.addTextChangedListener {
            binding.code.error = null
        }


        binding.methodFile.setOnClickListener {
            binding.code.isVisible = false

            if (billingViewModel.proStatus.value && App.prefs.showScrobbleSources) {
                val privateFileName = getString(
                    R.string.export_file_name,
                    "private_" + Stuff.getFileNameDateSuffix()
                )
                exportPrivateDataRequest.launch(privateFileName)
            }

            val fileName = getString(
                R.string.export_file_name,
                Stuff.getFileNameDateSuffix()
            )

            exportRequest.launch(fileName)
        }

        binding.methodNetwork.setOnClickListener {
            binding.code.isVisible = true
        }

        if (Stuff.isTv)
            binding.methodFile.isVisible = false

        collectLatestLifecycleFlow(viewModel.result.filterNotNull()) { result ->
            if (result.isFailure) {
                binding.code.error = result.exceptionOrNull()?.message
            } else {
                binding.code.error = null
                findNavController().popBackStack()
            }
        }
    }


    private fun export(currentUri: Uri?, privateData: Boolean = false) {
        currentUri ?: return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val exported = App.context.contentResolver.openOutputStream(currentUri)?.use {
                if (privateData)
                    viewModel.imExporter.exportPrivateData(it)
                else
                    viewModel.imExporter.export(it)
            } ?: false

            if (!exported)
                withContext(Dispatchers.Main) {
                    requireContext().toast(R.string.export_failed, Toast.LENGTH_LONG)
                }
            else
                Timber.i("Exported")
        }
    }

}