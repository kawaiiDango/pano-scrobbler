package com.arn.scrobble.billing


import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Layout
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.core.widget.TextViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.ExtrasConsts
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ContentBillingBinding
import com.arn.scrobble.main.App
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.dp
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.flow.filterNotNull


class BillingFragment : Fragment() {

    private val viewModel by activityViewModels<BillingViewModel>()
    private var _binding: ContentBillingBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.Y, MaterialSharedAxis.X)

        _binding = ContentBillingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.setupInsets()

        val bulletStrings = listOfNotNull(
            R.drawable.vd_palette to getString(R.string.pref_themes),
            R.drawable.vd_apps to getString(R.string.billing_scrobble_source),
            R.drawable.vd_ban to getString(R.string.billing_block),
            R.drawable.vd_pin to getString(R.string.billing_pin_friends, 10),
            if (!Stuff.isTv)
                R.drawable.vd_heart to getString(R.string.pref_link_heart_button_rating)
            else
                null,
            R.drawable.vd_extract to getString(R.string.billing_regex_extract),
            if (!Stuff.isTv)
                R.drawable.vd_share to getString(R.string.billing_sharing)
            else
                null,
        ).asReversed()

        bulletStrings.forEach { (iconRes, string) ->
            val textView = MaterialTextView(requireContext()).apply {
                text = string
                TextViewCompat.setCompoundDrawableTintList(
                    this,
                    ColorStateList.valueOf(
                        MaterialColors.getColor(
                            requireContext(),
                            com.google.android.material.R.attr.colorSecondary,
                            null
                        )
                    )
                )

                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    iconRes,
                    0,
                    0,
                    0
                )

                compoundDrawablePadding = 16.dp
                textSize = 16f
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    justificationMode = Layout.JUSTIFICATION_MODE_INTER_WORD
                }
                updatePaddingRelative(bottom = 4.dp)
            }
            binding.billingLl.addView(textView, 3)
        }

        binding.startBilling.setOnClickListener {
            val productDetails = viewModel.proProductDetails.value
            if (productDetails != null) {
                makePurchase()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.thank_you)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        findNavController().popBackStack(R.id.myHomePagerFragment, false)
                    }
                    .show()
            }
        }

        if (ExtrasConsts.isFossBuild) {
            binding.code.isVisible = true

            binding.code.setEndIconOnClickListener {
                App.prefs.receipt = binding.codeEdittext.text?.trim()?.toString()
                viewModel.queryPurchasesAsync()
            }

            binding.codeEdittext.inputType =
                EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_CAP_CHARACTERS

            binding.codeEdittext.setOnEditorActionListener { textView, actionId, keyEvent ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN)
                ) {
                    App.prefs.receipt = binding.codeEdittext.text?.trim()?.toString()
                    viewModel.queryPurchasesAsync()
                    true
                } else
                    false
            }

            binding.codeEdittext.addTextChangedListener {
                binding.code.error = null
            }
        }

        binding.billingTroubleshoot.setOnClickListener {
            findNavController().navigate(R.id.billingTroubleshootFragment)
        }

        collectLatestLifecycleFlow(viewModel.proProductDetails.filterNotNull()) {
            binding.startBilling.text = Html.fromHtml(
                "<big>" + getString(R.string.get_pro) + "</big>" +
                        "<br><small>" + it.formattedPrice + "</small>"
            )
        }

        collectLatestLifecycleFlow(viewModel.proPendingSince.filterNotNull()) {
//          This doesn't go away after the slow card gets declined. So, only notify recent purchases
            if (System.currentTimeMillis() - it < Stuff.PENDING_PURCHASE_NOTIFY_THRESHOLD)
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.purchase_pending)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        }

        collectLatestLifecycleFlow(viewModel.proStatus) {
            if (it) {
                requireContext().toast(R.string.thank_you)
                findNavController().popBackStack(R.id.myHomePagerFragment, false)
            }
        }
    }

    private fun makePurchase() {
        if (ExtrasConsts.isFossBuild) {
            Stuff.openInBrowser(requireContext(), getString(R.string.ko_fi_link))
        } else {
            viewModel.makePlayPurchase(requireActivity())
        }
    }
}