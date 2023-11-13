package com.arn.scrobble

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MenuRes
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.Stuff.getSingle
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.databinding.ContentOptionsMenuBinding
import com.arn.scrobble.ui.OptionsMenuVM
import com.arn.scrobble.ui.UiUtils
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
    private val dontDismissForTheseIds = setOf(
        R.id.nav_help,
    )
    private var selectedMenuItemId: Int? = null

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

        mainNotifierViewModel.updateCanIndex()
        if (mainNotifierViewModel.canIndex.value == false)
            binding.optionsMenuNav.menu.removeItem(R.id.nav_do_index)

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

            if (mainNotifierViewModel.isItChristmas)
                UiUtils.applySnowfall(
                    binding.headerNav.navProfilePic,
                    binding.headerNav.root,
                    layoutInflater,
                    viewLifecycleOwner.lifecycleScope
                )
        }

        binding.optionsMenuNav.setNavigationItemSelectedListener { menuItem ->
            selectedMenuItemId = menuItem.itemId
            if (menuItem.itemId !in dontDismissForTheseIds)
                dismiss()
            else
                optionsMenuViewModel.menuEvent.value =
                    binding.optionsMenuNav to selectedMenuItemId!!

            true
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        expandIfNeeded()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // if the new destination is a BottomSheetDialogFragment, both get dismissed otherwise
        selectedMenuItemId?.let {
            optionsMenuViewModel.menuEvent.value = binding.optionsMenuNav to it
        }
        _binding = null
    }
}

@Parcelize
data class OptionsMenuMetadata(
    @MenuRes val menuRes: Int,
    val showHeader: Boolean,
) : Parcelable