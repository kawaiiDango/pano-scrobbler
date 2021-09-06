package com.arn.scrobble.billing


import android.os.Bundle
import android.transition.Fade
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arn.scrobble.*
import com.arn.scrobble.databinding.ContentBillingBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class BillingFragment : Fragment() {

    private lateinit var billingViewModel: BillingViewModel
    private var _binding : ContentBillingBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reenterTransition = Fade()
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
        Stuff.setTitle(activity, "")

    }
    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.startBilling.setOnClickListener {
            val skuDetails = billingViewModel.proSkuDetails.value
            if (skuDetails != null)
                billingViewModel.makePurchase(activity!!, skuDetails)
            else {
                MaterialAlertDialogBuilder(context!!)
                    .setMessage(R.string.thank_you)
                    .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
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