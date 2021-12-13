package com.arn.scrobble.billing


import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.arn.scrobble.*
import com.arn.scrobble.databinding.ContentBillingBinding
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        Stuff.setTitle(activity!!, "")

    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bulletStrings = arrayOf(
            R.drawable.vd_palette to R.string.pref_themes,
            R.drawable.vd_apps to R.string.pref_show_scrobble_sources,
            R.drawable.vd_ban to R.string.billing_block,
            R.drawable.vd_share to R.string.billing_sharing
        )

        val spannableStringBuilder = SpannableStringBuilder()

        bulletStrings.forEach { (iconRes, stringRes) ->
            val l = spannableStringBuilder.length
            spannableStringBuilder.append("\timg " + getString(stringRes) + "\n")
            val icon = ContextCompat.getDrawable(context!!, iconRes)!!.apply {
                setTint(MaterialColors.getColor(context!!, R.attr.colorSecondary, null))
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
            val iconSpan = ImageSpan(icon, ImageSpan.ALIGN_BOTTOM)

            spannableStringBuilder.setSpan(
                iconSpan,
                l + 1,
                l + 4,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        binding.proBenefits.text = spannableStringBuilder.trimEnd()

        binding.startBilling.setOnClickListener {
            val skuDetails = billingViewModel.proSkuDetails.value
            if (skuDetails != null)
                billingViewModel.makePurchase(activity!!, skuDetails)
            else {
                MaterialAlertDialogBuilder(context!!)
                    .setMessage(R.string.thank_you)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        parentFragmentManager.popBackStack()
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
        billingViewModel.proSkuDetails.observe(viewLifecycleOwner) {
            it?.let {
                Stuff.log("price: " + it.priceCurrencyCode + it.price)
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
                Stuff.toast(context, getString(R.string.thank_you))
                if (!BuildConfig.DEBUG)
                    parentFragmentManager.popBackStack()
            }
        }
    }
}