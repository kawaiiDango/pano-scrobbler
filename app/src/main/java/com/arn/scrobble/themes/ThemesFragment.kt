package com.arn.scrobble.themes

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.fragment.app.Fragment
import com.arn.scrobble.*
import com.arn.scrobble.Stuff.dp
import com.arn.scrobble.Stuff.setArrowColors
import com.arn.scrobble.billing.BillingFragment
import com.arn.scrobble.databinding.ContentThemesBinding
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.themes.ColorPatchUtils.getStyledColor
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors

class ThemesFragment : Fragment() {

    private var _binding: ContentThemesBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var primarySwatchIds: List<Int>
    private lateinit var secondarySwatchIds: List<Int>
    private val prefs by lazy { MainPrefs(context!!) }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentThemesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var primaryPref = prefs.themePrimary
        if (ColorPatchMap.primaryStyles[primaryPref] == null)
            primaryPref = ColorPatchUtils.primaryDefault

        var secondaryPref = prefs.themeSecondary
        if (ColorPatchMap.secondaryStyles[secondaryPref] == null)
            secondaryPref = ColorPatchUtils.secondaryDefault

        binding.themeRandom.isChecked = prefs.themeRandom
        binding.themePaletteBg.isChecked = prefs.themePaletteBackground
        binding.themeTintBg.isChecked = prefs.themeTintBackground
        binding.themeDynamic.isChecked = prefs.themeDynamic

        primarySwatchIds = createSwatches(
            styles = ColorPatchMap.primaryStyles,
            group = binding.themePrimarySwatches,
            attr = R.attr.colorPrimary,
            isDark = false,
            selectedName = primaryPref,
        )
        secondarySwatchIds = createSwatches(
            styles = ColorPatchMap.secondaryStyles,
            group = binding.themeSecondarySwatches,
            attr = R.attr.colorSecondary,
            isDark = false,
            selectedName = secondaryPref,
        )

        setSwatchesEnabled(!(binding.themeRandom.isChecked || binding.themeDynamic.isChecked))
        setCheckboxesEnabled(!binding.themeDynamic.isChecked)

        binding.themePrimarySwatches.setOnCheckedChangeListener { group, checkedId ->
            val name = group.findViewById<Chip>(checkedId)?.contentDescription
                ?: return@setOnCheckedChangeListener
            previewPrimary(name.toString())
        }

        binding.themeSecondarySwatches.setOnCheckedChangeListener { group, checkedId ->
            val name = group.findViewById<Chip>(checkedId)?.contentDescription
                ?: return@setOnCheckedChangeListener
            previewSecondary(name.toString())
        }

        binding.themeRandom.setOnCheckedChangeListener { compoundButton, checked ->
            setSwatchesEnabled(!checked)
        }

        binding.themeDone.setOnClickListener {
            if ((activity as MainActivity).billingViewModel.proStatus.value == true) {
                saveTheme()
                if (!binding.themeRandom.isChecked)
                    context!!.sendBroadcast(
                        Intent(NLService.iTHEME_CHANGED_S),
                        NLService.BROADCAST_PERMISSION
                    )
                parentFragmentManager.popBackStack()
                activity!!.recreate()
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frame, BillingFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        if (BuildConfig.DEBUG || DynamicColors.isDynamicColorAvailable()) {
            binding.themeDynamic.visibility = View.VISIBLE
            binding.themeDynamic.setOnCheckedChangeListener { compoundButton, checked ->
                setSwatchesEnabled(!checked)
                setCheckboxesEnabled(!checked)
            }
        }

        binding.themeTintBg.setOnCheckedChangeListener { compoundButton, checked ->
            val name = binding.themePrimarySwatches.findViewById<Chip>(binding.themePrimarySwatches.checkedChipId)?.contentDescription
                ?: return@setOnCheckedChangeListener
            previewPrimary(name.toString())
        }
    }

    override fun onDestroyView() {
        val act = activity as MainActivity
        act.binding.coordinatorMain.ctl.contentScrim =
            ColorDrawable(MaterialColors.getColor(context!!, android.R.attr.colorBackground, null))
        act.binding.coordinatorMain.toolbar.setTitleTextColor(
            MaterialColors.getColor(context!!, R.attr.colorPrimary, null)
        )
        act.binding.coordinatorMain.toolbar.setArrowColors(
            MaterialColors.getColor(activity, R.attr.colorPrimary, null),
            Color.TRANSPARENT
        )
        _binding = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity!!, getString(R.string.pref_themes))
    }

    private fun createSwatches(
        styles: Map<String, Int>,
        group: ChipGroup,
        @AttrRes attr: Int,
        isDark: Boolean,
        selectedName: String
    ): List<Int> {
        val idList = mutableListOf<Int>()
        styles.forEach { (name, style) ->
            val color = context!!.getStyledColor(style, attr)
            val id = View.generateViewId()
            idList.add(id)
            val swatch =
                (layoutInflater.inflate(R.layout.chip_swatch, group, false) as Chip).apply {
                    chipBackgroundColor = ColorStateList.valueOf(color)
                    checkedIconTint =
                        ColorStateList.valueOf(if (isDark) Color.WHITE else Color.BLACK)
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
        val colorPrimary = context!!.getStyledColor(styleId, R.attr.colorPrimary)
        val colorPrimaryContainer = context!!.getStyledColor(styleId, R.attr.colorPrimaryContainer)
        val colorOnPrimaryContainer = context!!.getStyledColor(styleId, R.attr.colorOnPrimaryContainer)
        binding.themeDone.backgroundTintList = ColorStateList.valueOf(colorPrimaryContainer)
        binding.themeDone.supportImageTintList = ColorStateList.valueOf(colorOnPrimaryContainer)
        val act = activity as MainActivity
        act.binding.coordinatorMain.toolbar.setTitleTextColor(colorPrimary)
        act.binding.coordinatorMain.toolbar.setArrowColors(
            colorPrimary,
            Color.TRANSPARENT
        )
        if (binding.themeTintBg.isChecked)
            previewBackground(name)
        else
            previewBackground(ColorPatchUtils.backgroundBlack)
    }

    private fun previewSecondary(name: String) {
        val styleId = ColorPatchMap.secondaryStyles[name]!!
        val color = context!!.getStyledColor(styleId, R.attr.colorSecondary)
        binding.themePrimaryHeader.setTextColor(color)
        binding.themeSecondaryHeader.setTextColor(color)
        binding.themeRandom.buttonTintList = ColorStateList.valueOf(color)
        binding.themeDynamic.buttonTintList = ColorStateList.valueOf(color)
        binding.themeTintBg.buttonTintList = ColorStateList.valueOf(color)
        binding.themePaletteBg.buttonTintList = ColorStateList.valueOf(color)
    }

    private fun previewBackground(name: String) {
        val styleId = ColorPatchMap.backgroundStyles[name]!!
        val color = context!!.getStyledColor(styleId, android.R.attr.colorBackground)
        binding.root.background = ColorDrawable(color)
        val act = activity as MainActivity
        act.window.navigationBarColor = color
        act.binding.coordinatorMain.ctl.contentScrim = ColorDrawable(color)
    }

    private fun setCheckboxesEnabled(enabled: Boolean) {
        binding.themeRandom.isEnabled = enabled
        binding.themeTintBg.isEnabled = enabled
    }

    private fun setSwatchesEnabled(enabled: Boolean) {

        fun setEnabled(vararg vgs: ViewGroup) {
            for (vg in vgs) {
                if (vg.isEnabled != enabled) {
                    val alpha = if (enabled) 1f else 0.5f
                    vg.isEnabled = enabled
                    vg.alpha = alpha
                    for (i in 0 until vg.childCount) {
                        val v = vg.getChildAt(i)!!
                        v.isEnabled = enabled
                    }
                }
            }
        }

        setEnabled(
            binding.themePrimarySwatches,
            binding.themeSecondarySwatches
        )
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
            themePaletteBackground = binding.themePaletteBg.isChecked
            themeTintBackground = binding.themeTintBg.isChecked
            themeDynamic = binding.themeDynamic.isChecked
        }
    }
}