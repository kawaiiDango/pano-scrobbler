package com.arn.scrobble.themes

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.NLService
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ContentThemesBinding
import com.arn.scrobble.main.FabData
import com.arn.scrobble.main.MainActivityOld
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.themes.ColorPatchUtils.getStyledColor
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.dp
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ThemesFragment : Fragment() {

    private var _binding: ContentThemesBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var primarySwatchIds: List<Int>
    private lateinit var secondarySwatchIds: List<Int>
    private val mainPrefs = PlatformStuff.mainPrefs
    private val dayNightIdsMap by lazy {
        mapOf(
            R.id.chip_dark to DayNightMode.DARK,
            R.id.chip_light to DayNightMode.LIGHT,
            R.id.chip_auto to DayNightMode.SYSTEM
        )
    }
    private val mainNotifierViewModel by activityViewModels<MainViewModel>()

    private lateinit var themedContext: ContextThemeWrapper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupAxisTransitions(MaterialSharedAxis.X)

        _binding = ContentThemesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.setupInsets()

        val prefs = runBlocking {
            mainPrefs.data.first()
        }
        var primaryPref = ColorPatchUtils.primaryDefault

        var secondaryPref = ColorPatchUtils.secondaryDefault



        binding.themeDynamic.isChecked = prefs.themeDynamic

        val dayNightSelectedId = dayNightIdsMap
            .firstNotNullOfOrNull { (id, v) ->
                if (v == prefs.themeDayNight) id else null
            } ?: dayNightIdsMap.keys.first()

        binding.themeDayNight.check(dayNightSelectedId)

        themedContext = ContextThemeWrapper(requireContext(), R.style.AppTheme)

        primarySwatchIds = createSwatches(
            styles = ColorPatchMap.primaryStyles,
            group = binding.themePrimarySwatches,
            attr = com.google.android.material.R.attr.colorPrimary,
            selectedName = primaryPref,
        )
        secondarySwatchIds = createSwatches(
            styles = ColorPatchMap.secondaryStyles,
            group = binding.themeSecondarySwatches,
            attr = com.google.android.material.R.attr.colorSecondary,
            selectedName = secondaryPref,
        )

        setSwatchesEnabled(!(binding.themeRandom.isChecked || binding.themeDynamic.isChecked))
        setCheckboxesEnabled(!binding.themeDynamic.isChecked)

        binding.themePrimarySwatches.setOnCheckedStateChangeListener { group, checkedIds ->
            previewPrimary(getThemeName(group))
        }

        binding.themeSecondarySwatches.setOnCheckedStateChangeListener { group, checkedIds ->
            previewSecondary(getThemeName(group))
        }

        binding.themeRandom.setOnCheckedChangeListener { compoundButton, checked ->
            setSwatchesEnabled(!checked)
        }

        val fabData = FabData(
            viewLifecycleOwner,
            com.google.android.material.R.string.abc_action_mode_done,
            R.drawable.vd_check_simple,
            {
                if (Stuff.billingRepository.isLicenseValid) {

                    val prevDayNightId = prefs.themeDayNight
                    viewLifecycleOwner.lifecycleScope.launch {
                        saveTheme()
                    }
                    if (!binding.themeRandom.isChecked)
                        requireContext().sendBroadcast(
                            Intent(NLService.iTHEME_CHANGED_S)
                                .setPackage(requireContext().packageName),
                            NLService.BROADCAST_PERMISSION
                        )
                    findNavController().popBackStack()

                    if (prefs.themeDayNight != prevDayNightId)
                        ColorPatchUtils.setDarkMode() // recreates
                    else
                        requireActivity().recreate()
                } else {
                    findNavController().navigate(R.id.billingFragment)
                }
            })

        mainNotifierViewModel.setFabData(fabData)

        if (DynamicColors.isDynamicColorAvailable() && !Stuff.isTv) {
            binding.themeDynamic.visibility = View.VISIBLE
            binding.themeDynamic.setOnCheckedChangeListener { compoundButton, checked ->
                setSwatchesEnabled(!checked)
                setCheckboxesEnabled(!checked)
            }
        }

        binding.themeTintBg.setOnCheckedChangeListener { compoundButton, checked ->
            previewPrimary(getThemeName(binding.themePrimarySwatches))
        }

        binding.themeDayNight.setOnCheckedStateChangeListener { group, checkedIds ->
            updateThemedContext()
            val primary = getThemeName(binding.themePrimarySwatches)
            val secondary = getThemeName(binding.themeSecondarySwatches)

            previewPrimary(primary)
            previewSecondary(secondary)
            previewChipColors()
            previewTextColors()
            previewSwatchesColors()
        }

        if (Stuff.isTv) {
            binding.themeDayNight.isVisible = false
            binding.themeDayNight.check(R.id.chip_dark)
        }
    }

    override fun onDestroyView() {
        val act = activity as MainActivityOld
        act.binding.ctl.background = ColorDrawable(Color.TRANSPARENT)
        act.binding.ctl.setExpandedTitleColor(
            MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorPrimary,
                null
            )
        )
        act.binding.ctl.setCollapsedTitleTextColor(
            MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorPrimary,
                null
            )
        )
        _binding = null
        super.onDestroyView()
    }

    private fun updateThemedContext() {
        themedContext = ContextThemeWrapper(requireContext(), R.style.AppTheme)
        themedContext.applyOverrideConfiguration(
            Configuration().apply {
                setToDefaults()
                val nightBits = when (binding.themeDayNight.checkedChipId) {
                    R.id.chip_light -> Configuration.UI_MODE_NIGHT_NO
                    R.id.chip_dark -> Configuration.UI_MODE_NIGHT_YES
                    else -> Configuration.UI_MODE_NIGHT_UNDEFINED
                }
                uiMode = uiMode or nightBits
            }
        )
    }

    private fun createSwatches(
        styles: Map<String, Int>,
        group: ChipGroup,
        @AttrRes attr: Int,
        selectedName: String
    ): List<Int> {
        val idList = mutableListOf<Int>()

        styles.forEach { (name, style) ->
            val color = themedContext.getStyledColor(style, attr)
            val id = View.generateViewId()
            idList.add(id)
            val swatch =
                (layoutInflater.inflate(R.layout.chip_swatch, group, false) as Chip).apply {
                    chipBackgroundColor = ColorStateList.valueOf(color)
                    this.id = id
                    contentDescription = name
                    setOnCheckedChangeListener { chip, checked ->
                        chip as Chip
                        chip.chipStrokeWidth = if (checked) 4.dp.toFloat() else 2.dp.toFloat()
                    }
                    isChecked = selectedName == name
                }
            group.addView(swatch)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                swatch.tooltipText = name
            }
        }
        return idList
    }

    private fun previewPrimary(name: String) {
        val styleId = ColorPatchMap.primaryStyles[name]!!
        val colorPrimary =
            themedContext.getStyledColor(styleId, com.google.android.material.R.attr.colorPrimary)
        val act = activity as MainActivityOld
        act.binding.ctl.setExpandedTitleColor(colorPrimary)
        act.binding.ctl.setCollapsedTitleTextColor(colorPrimary)
        if (binding.themeTintBg.isChecked)
            previewBackground(name)
        else
            previewBackground(R.style.ColorPatchManual_Pure_Background)

        previewChipColors()
    }

    private fun previewSecondary(name: String) {
        val styleId = ColorPatchMap.secondaryStyles[name]!!
        val color =
            themedContext.getStyledColor(styleId, com.google.android.material.R.attr.colorSecondary)
        binding.themePrimaryHeader.setTextColor(color)
        binding.themeSecondaryHeader.setTextColor(color)

        val buttonTintList = ContextCompat.getColorStateList(
            themedContext,
            com.google.android.material.R.color.m3_checkbox_button_tint
        )

        val iconTintList = ContextCompat.getColorStateList(
            themedContext,
            com.google.android.material.R.color.m3_checkbox_button_icon_tint
        )

        arrayOf(
            binding.themeRandom,
            binding.themeDynamic,
            binding.themeTintBg
        ).forEach {
            it.buttonTintList = buttonTintList
            it.buttonIconTintList = iconTintList
        }

        previewChipColors()
    }

    private fun previewChipColors() {
        val primary = getThemeName(binding.themePrimarySwatches)
        val secondary = getThemeName(binding.themeSecondarySwatches)

        val primaryStyleId = ColorPatchMap.primaryStyles[primary]!!
        val secondaryStyleId = ColorPatchMap.secondaryStyles[secondary]!!

        val primaryColor = themedContext.getStyledColor(
            primaryStyleId,
            com.google.android.material.R.attr.colorSurface
        )
        val secondaryColor =
            themedContext.getStyledColor(
                secondaryStyleId,
                com.google.android.material.R.attr.colorSecondaryContainer
            )
        val rippleColor = ContextCompat.getColorStateList(
            themedContext,
            com.google.android.material.R.color.m3_chip_ripple_color
        )
        val strokeColor = ContextCompat.getColorStateList(
            themedContext,
            com.google.android.material.R.color.m3_chip_stroke_color
        )

        val states = arrayOf(
            intArrayOf(-android.R.attr.state_checked), // unchecked
            intArrayOf(android.R.attr.state_checked), // checked
//            intArrayOf(android.R.attr.state_selected), // selected
        )
        val bgColors = intArrayOf(primaryColor, secondaryColor)

        val textColor = ContextCompat.getColorStateList(
            themedContext,
            com.google.android.material.R.color.m3_chip_text_color
        )

        binding.themeDayNight.children.forEach {
            it as Chip
            it.chipBackgroundColor = ColorStateList(states, bgColors)
            it.setTextColor(textColor)
            it.chipStrokeColor = strokeColor
            it.rippleColor = rippleColor
        }
    }

    private fun previewBackground(name: String) {
        val styleId = ColorPatchMap.backgroundStyles[name]!!
        previewBackground(styleId)
    }

    private fun previewBackground(@StyleRes styleId: Int) {
        val color = themedContext.getStyledColor(styleId, android.R.attr.colorBackground)
        binding.root.background = ColorDrawable(color)
        val act = activity as MainActivityOld
//        act.binding.ctl.contentScrim = ColorDrawable(color)
        act.binding.ctl.background = ColorDrawable(color)
    }

    private fun previewTextColors() {
        val color = MaterialColors.getColor(
            themedContext,
            com.google.android.material.R.attr.colorOnPrimarySurface,
            null
        )
        arrayOf(
            binding.themeRandom,
            binding.themeDynamic,
            binding.themeTintBg,
        ).forEach {
            it.setTextColor(color)
        }
    }

    private fun setCheckboxesEnabled(enabled: Boolean) {
        binding.themeRandom.isEnabled = enabled
    }

    private fun setSwatchesEnabled(enabled: Boolean) {
        arrayOf(
            binding.themePrimarySwatches,
            binding.themeSecondarySwatches
        ).forEach { vg ->
            if (vg.isEnabled != enabled) {
                val alpha = if (enabled) 1f else 0.5f
                vg.isEnabled = enabled
                vg.alpha = alpha
                vg.children.forEach {
                    it as Chip
                    it.isEnabled = enabled
                }
            }
        }
    }

    private fun previewSwatchesColors() {
        val primary = getThemeName(binding.themePrimarySwatches)

        val primaryStyleId = ColorPatchMap.primaryStyles[primary]!!

        // Define the states
        val states = arrayOf(
            intArrayOf(android.R.attr.state_focused), // State when the item is focused
            intArrayOf(-android.R.attr.state_focused) // State when the item is not focused
        )

        // Define the colors for each state
        val colors = intArrayOf(
            themedContext.getStyledColor(
                primaryStyleId,
                com.google.android.material.R.attr.colorPrimaryContainer
            ),
            ContextCompat.getColor(themedContext, R.color.foreground_pure)
        )

        // Create the ColorStateList
        val strokeColor = ColorStateList(states, colors)
        val checkColor = ContextCompat.getColor(themedContext, R.color.background_pure)

        arrayOf(
            binding.themePrimarySwatches,
            binding.themeSecondarySwatches
        ).forEach { vg ->
            vg.children.forEach {
                it as Chip
                it.chipStrokeColor = strokeColor
                it.checkedIconTint = ColorStateList.valueOf(checkColor)
                it.chipBackgroundColor = ColorStateList.valueOf(
                    themedContext.getStyledColor(
                        ColorPatchMap.primaryStyles[it.contentDescription]!!,
                        com.google.android.material.R.attr.colorPrimary
                    )
                )
            }
        }
    }

    private fun getThemeName(group: ChipGroup): String {
        val id = group.checkedChipId
        return group.findViewById<Chip>(id).contentDescription.toString()
    }

    private suspend fun saveTheme() {
//        mainPrefs.updateData {
//            it.copy(
//                themePrimary = getThemeName(binding.themePrimarySwatches),
//                themeSecondary = getThemeName(binding.themeSecondarySwatches),
//                themeRandom = binding.themeRandom.isChecked,
//                themeDynamic = binding.themeDynamic.isChecked,
//                themeTintBackground = binding.themeTintBg.isChecked,
//                themeDayNight = dayNightIdsMap[binding.themeDayNight.checkedChipId]!!
//            )
//        }
    }
}