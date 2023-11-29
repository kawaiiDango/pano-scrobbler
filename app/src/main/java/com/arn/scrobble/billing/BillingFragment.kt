package com.arn.scrobble.billing


import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePaddingRelative
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ContentBillingBinding
import com.arn.scrobble.ui.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.ui.UiUtils.dp
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.ui.UiUtils.toast
import com.arn.scrobble.utils.Stuff
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.flow.filterNotNull


class BillingFragment : Fragment() {

    private val billingViewModel by activityViewModels<BillingViewModel>()
    private var _binding: ContentBillingBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAxisTransitions(MaterialSharedAxis.Y, MaterialSharedAxis.X)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentBillingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.setupInsets()

        val bulletStrings = arrayOf(
            R.drawable.vd_palette to getString(R.string.pref_themes),
            R.drawable.vd_apps to getString(R.string.billing_scrobble_source),
            R.drawable.vd_ban to getString(R.string.billing_block),
            R.drawable.vd_pin to getString(R.string.billing_pin_friends, 10),
            R.drawable.vd_extract to getString(R.string.billing_regex_extract),
            R.drawable.vd_share to getString(R.string.billing_sharing),
        ).reversedArray()

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
            val productDetails = billingViewModel.proProductDetails.value
            if (productDetails != null)
                billingViewModel.makePurchase(requireActivity(), productDetails)
            else {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.thank_you)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        findNavController().popBackStack(R.id.myHomePagerFragment, false)
                    }
                    .show()
            }
        }

        binding.billingTroubleshoot.setOnClickListener {
            findNavController().navigate(R.id.billingTroubleshootFragment)
        }

        collectLatestLifecycleFlow(billingViewModel.proProductDetails.filterNotNull()) {
            binding.startBilling.text = Html.fromHtml(
                "<big>" + getString(R.string.get_pro) + "</big>" +
                        "<br><small>" + it.oneTimePurchaseOfferDetails!!.formattedPrice + "</small>"
            )
        }

        collectLatestLifecycleFlow(billingViewModel.proPendingSince.filterNotNull()) {
//          This doesn't go away after the slow card gets declined. So, only notify recent purchases
            if (System.currentTimeMillis() - it < Stuff.PENDING_PURCHASE_NOTIFY_THRESHOLD)
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.purchase_pending)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        }

        collectLatestLifecycleFlow(billingViewModel.proStatus) {
            if (it) {
                requireContext().toast(R.string.thank_you)
                if (!BuildConfig.DEBUG)
                    findNavController().popBackStack(R.id.myHomePagerFragment, false)
            }
        }
    }
}