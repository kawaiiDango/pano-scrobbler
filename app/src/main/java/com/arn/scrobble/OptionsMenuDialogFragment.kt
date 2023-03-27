package com.arn.scrobble

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MenuRes
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.Stuff.getSingle
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.databinding.ContentOptionsMenuBinding
import com.arn.scrobble.ui.OptionsMenuVM
import com.arn.scrobble.ui.UiUtils.expandIfNeeded
import com.arn.scrobble.ui.UiUtils.scheduleTransition
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.parcelize.Parcelize

class OptionsMenuDialogFragment : BottomSheetDialogFragment() {

    private val billingViewModel by activityViewModels<BillingViewModel>()
    private val optionsMenuViewModel by activityViewModels<OptionsMenuVM>()
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()
    private var _binding: ContentOptionsMenuBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentOptionsMenuBinding.inflate(inflater, container, false)
        val metadata = requireArguments().getSingle<OptionsMenuMetadata>()!!
        binding.optionsMenuNav.inflateMenu(metadata.menuRes)

        if (billingViewModel.proStatus.value == true) {
            binding.optionsMenuNav.menu.removeItem(R.id.nav_pro)
        }

        if (metadata.showHeader) {
            binding.headerNav.root.isVisible = true
            mainNotifierViewModel.drawerData.observe(viewLifecycleOwner) {
                it ?: return@observe
                scheduleTransition()
                NavUtils.updateHeaderWithDrawerData(binding.headerNav, mainNotifierViewModel)
            }
            mainNotifierViewModel.loadCurrentUserDrawerData()
            NavUtils.setProfileSwitcher(
                binding.headerNav,
                findNavController(),
                mainNotifierViewModel
            )
            binding.headerNav.navDivider.isVisible = false
        }

        binding.optionsMenuNav.setNavigationItemSelectedListener { menuItem ->
            dismiss()
            optionsMenuViewModel.menuEvent.value = menuItem.itemId
            true
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        expandIfNeeded()
    }
}

@Parcelize
data class OptionsMenuMetadata(
    @MenuRes val menuRes: Int,
    val showHeader: Boolean,
) : Parcelable