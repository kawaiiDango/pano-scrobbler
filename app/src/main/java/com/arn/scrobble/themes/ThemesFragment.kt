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
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.App
import com.arn.scrobble.MainActivity
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.databinding.ContentThemesBinding
import com.arn.scrobble.themes.ColorPatchUtils.getStyledColor
import com.arn.scrobble.ui.FabData
import com.arn.scrobble.ui.UiUtils.dp
import com.arn.scrobble.ui.UiUtils.setupAxisTransitions
import com.arn.scrobble.ui.UiUtils.setupInsets
import com.arn.scrobble.utils.Stuff.firstOrNull
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.MaterialSharedAxis
import io.michaelrocks.bimap.HashBiMap

class ThemesFragment : Fragment() {

    private var _binding: ContentThemesBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var primarySwatchIds: List<Int>
    private lateinit var secondarySwatchIds: List<Int>
    private val prefs = App.prefs
    private val dayNightIdsMap by lazy {
        HashBiMap.create(
            mapOf(
                R.id.chip_dark to AppCompatDelegate.MODE_NIGHT_YES,
                R.id.chip_light to AppCompatDelegate.MODE_NIGHT_NO,
                R.id.chip_auto to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
        )
    }
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()
    private val billingViewModel by activityViewModels<BillingViewModel>()

    private lateinit var themedContext: ContextThemeWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupAxisTransitions(MaterialSharedAxis.X)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentThemesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.root.setupInsets()

        var primaryPref = prefs.themePrimary
        if (ColorPatchMap.primaryStyles[primaryPref] == null)
            primaryPref = ColorPatchUtils.primaryDefault

        var secondaryPref = prefs.themeSecondary
        if (ColorPatchMap.secondaryStyles[secondaryPref] == null)
            secondaryPref = ColorPatchUtils.secondaryDefault


        binding.themeRandom.isChecked = prefs.themeRandom
        binding.themeTintBg.isChecked = prefs.themeTintBackground
        binding.themeDynamic.isChecked = prefs.themeDynamic

        val dayNightSelectedId =
            dayNightIdsMap.inverse[prefs.themeDayNight] ?: dayNightIdsMap.firstOrNull()!!

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
                if (billingViewModel.proStatus.value == true) {

                    val prevDayNightId = prefs.themeDayNight
                    saveTheme()
                    if (!binding.themeRandom.isChecked)
                        requireContext().sendBroadcast(
                            Intent(NLService.iTHEME_CHANGED_S)
                                .setPackage(requireContext().packageName),
                            NLService.BROADCAST_PERMISSION
                        )
                    findNavController().popBackStack()

                    if (prefs.themeDayNight != prevDayNightId)
                        ColorPatchUtils.setDarkMode(true) // recreates
                    else
                        requireActivity().recreate()
                } else {
                    findNavController().navigate(R.id.billingFragment)
                }
            })

        mainNotifierViewModel.setFabData(fabData)

        if (DynamicColors.isDynamicColorAvailable()) {
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
    }

    override fun onDestroyView() {
        val act = activity as MainActivity
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
        val colorPrimaryContainer =
            themedContext.getStyledColor(
                styleId,
                com.google.android.material.R.attr.colorPrimaryContainer
            )
        val colorOnPrimaryContainer =
            themedContext.getStyledColor(
                styleId,
                com.google.android.material.R.attr.colorOnPrimaryContainer
            )
//        binding.themeDone.backgroundTintList = ColorStateList.valueOf(colorPrimaryContainer)
//        binding.themeDone.supportImageTintList = ColorStateList.valueOf(colorOnPrimaryContainer)
        val act = activity as MainActivity
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
        binding.themeRandom.buttonTintList = ColorStateList.valueOf(color)
        binding.themeDynamic.buttonTintList = ColorStateList.valueOf(color)
        binding.themeTintBg.buttonTintList = ColorStateList.valueOf(color)

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

        val states = arrayOf(
            intArrayOf(-android.R.attr.state_checked), // unchecked
            intArrayOf(android.R.attr.state_checked), // unchecked
        )
        val colors = intArrayOf(primaryColor, secondaryColor)

        val textColor = MaterialColors.getColor(
            themedContext,
            com.google.android.material.R.attr.colorOnPrimarySurface,
            null
        )
        val outlineColor = themedContext.getStyledColor(
            secondaryStyleId,
            com.google.android.material.R.attr.colorOutline
        )

        binding.themeDayNight.children.forEach {
            it as Chip
            it.chipBackgroundColor = ColorStateList(states, colors)
            it.setTextColor(textColor)
            it.chipStrokeColor = ColorStateList.valueOf(outlineColor)
        }
    }

    private fun previewBackground(name: String) {
        val styleId = ColorPatchMap.backgroundStyles[name]!!
        previewBackground(styleId)
    }

    private fun previewBackground(@StyleRes styleId: Int) {
        val color = themedContext.getStyledColor(styleId, android.R.attr.colorBackground)
        binding.root.background = ColorDrawable(color)
        val act = activity as MainActivity
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
                    it.isEnabled = enabled
                }
            }
        }
    }

    private fun previewSwatchesColors() {
        val strokeColor = ContextCompat.getColor(themedContext, R.color.foreground_pure)
        val checkColor = ContextCompat.getColor(themedContext, R.color.background_pure)
        arrayOf(
            binding.themePrimarySwatches,
            binding.themeSecondarySwatches
        ).forEach { vg ->
            vg.children.forEach {
                it as Chip
                it.chipStrokeColor = ColorStateList.valueOf(strokeColor)
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

    private fun saveTheme() {
        prefs.apply {
            themePrimary = getThemeName(binding.themePrimarySwatches)
            themeSecondary = getThemeName(binding.themeSecondarySwatches)
            themeRandom = binding.themeRandom.isChecked
            themeDynamic = binding.themeDynamic.isChecked
            themeTintBackground = binding.themeTintBg.isChecked
            themeDayNight = dayNightIdsMap[binding.themeDayNight.checkedChipId]!!
        }
    }
}