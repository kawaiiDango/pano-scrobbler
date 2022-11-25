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
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.MainActivity
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ContentBillingBinding
import com.arn.scrobble.ui.UiUtils.dp
import com.arn.scrobble.ui.UiUtils.popBackStackTill
import com.arn.scrobble.ui.UiUtils.setTitle
import com.arn.scrobble.ui.UiUtils.toast
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.transition.MaterialSharedAxis


class BillingFragment : Fragment() {

    private lateinit var billingViewModel: BillingViewModel
    private var _binding: ContentBillingBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentBillingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        setTitle("")
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bulletStrings = arrayOf(
            R.drawable.vd_palette to getString(R.string.pref_themes),
            R.drawable.vd_apps to getString(R.string.billing_scrobble_source),
            R.drawable.vd_ban to getString(R.string.billing_block),
            R.drawable.vd_pin to getString(R.string.billing_pin_friends, 10),
            R.drawable.vd_share to getString(R.string.billing_sharing),
        )

        bulletStrings.forEach { (iconRes, string) ->
            val textView = MaterialTextView(context!!).apply {
                text = string
                TextViewCompat.setCompoundDrawableTintList(
                    this,
                    ColorStateList.valueOf(
                        MaterialColors.getColor(
                            context!!,
                            R.attr.colorSecondary,
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
            binding.billingLl.addView(textView)
        }

        binding.startBilling.setOnClickListener {
            val productDetails = billingViewModel.proProductDetails.value
            if (productDetails != null)
                billingViewModel.makePurchase(activity!!, productDetails)
            else {
                MaterialAlertDialogBuilder(context!!)
                    .setMessage(R.string.thank_you)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        parentFragmentManager.popBackStackTill(0)
                    }
                    .show()
            }
        }

        binding.billingTroubleshoot.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame, BillingTroubleshootFragment())
                .addToBackStack(null)
                .commit()
        }
        /*
                Picasso.get()
                    .load("https://i.imgur.com/DwRQl3l.png")
                    .into(binding.themesImg)

                Picasso.get()
                    .load("https://i.imgur.com/cYMoA6u.png")
                    .into(binding.shareImage)
        */
        billingViewModel = (activity as MainActivity).billingViewModel
        billingViewModel.proProductDetails.observe(viewLifecycleOwner) {
            it?.let {
                binding.startBilling.text = Html.fromHtml(
                    "<big>" + getString(R.string.get_pro) + "</big>" +
                            "<br><small>" + it.oneTimePurchaseOfferDetails!!.formattedPrice + "</small>"
                )
            }
        }
        billingViewModel.proPendingSince.observe(viewLifecycleOwner) {
            it ?: return@observe
//          This doesn't go away after the slow card gets declined. So, only notify recent purchases
            if (System.currentTimeMillis() - it < Stuff.PENDING_PURCHASE_NOTIFY_THRESHOLD)
                MaterialAlertDialogBuilder(context!!)
                    .setMessage(R.string.purchase_pending)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        }
        billingViewModel.proStatus.observe(viewLifecycleOwner) {
            if (it == true) {
                context!!.toast(R.string.thank_you)
                if (!BuildConfig.DEBUG)
                    parentFragmentManager.popBackStack()
            }
        }
    }
}