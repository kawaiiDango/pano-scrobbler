package com.arn.scrobble.onboarding

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.arn.scrobble.R
import com.arn.scrobble.databinding.DialogChangelogBinding
import com.arn.scrobble.utils.UiUtils.expandIfNeeded
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class ChangelogDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogChangelogBinding? = null
    private val binding get() = _binding!!
    private val args by navArgs<ChangelogDialogFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogChangelogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            expandIfNeeded(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.changelogText.text = args.changelogText ?: getString(R.string.changelog_text)
    }
}