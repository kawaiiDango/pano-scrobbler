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
import androidx.fragment.app.Fragment
import com.arn.scrobble.Main
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.setArrowColors
import com.arn.scrobble.billing.BillingFragment
import com.arn.scrobble.databinding.ContentThemesBinding
import com.arn.scrobble.pref.MultiPreferences
import com.arn.scrobble.themes.ColorPatchUtils.getStyledColor
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors

class ThemesFragment: Fragment() {

    private var _binding: ContentThemesBinding? = null
    private val binding
        get() = _binding!!

    lateinit var primarySwatchIds: MutableList<Int>
    lateinit var secondarySwatchIds: MutableList<Int>
    lateinit var backgroundSwatchIds: MutableList<Int>

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

        val pref = MultiPreferences(context!!)

        var primaryPref = pref.getString(Stuff.PREF_THEME_PRIMARY, ColorPatchUtils.primaryDefault)!!
        if (ColorPatchMap.primaryStyles[primaryPref] == null)
            primaryPref = ColorPatchUtils.primaryDefault

        var secondaryPref = pref.getString(Stuff.PREF_THEME_SECONDARY, ColorPatchUtils.secondaryDefault)!!
        if (ColorPatchMap.secondaryStyles[secondaryPref] == null)
            secondaryPref = ColorPatchUtils.secondaryDefault

        var backgroundPref = pref.getString(Stuff.PREF_THEME_BACKGROUND, ColorPatchUtils.backgroundDefault)!!
        if (ColorPatchMap.backgroundStyles[backgroundPref] == null)
            backgroundPref = ColorPatchUtils.backgroundDefault

        binding.themeRandom.isChecked = pref.getBoolean(Stuff.PREF_THEME_RANDOM, false)
        binding.themeSameTone.isChecked = pref.getBoolean(Stuff.PREF_THEME_SAME_TONE, false)

        primarySwatchIds = mutableListOf()
        secondarySwatchIds = mutableListOf()
        backgroundSwatchIds = mutableListOf()

        ColorPatchMap.primaryStyles.forEach { (name, style) ->
            val swatch = layoutInflater.inflate(R.layout.chip_swatch, binding.themePrimarySwatches, false) as Chip
            val color = context!!.getStyledColor(style, R.attr.colorPrimary)
            swatch.chipBackgroundColor = ColorStateList.valueOf(color)
            swatch.checkedIconTint = ColorStateList.valueOf(Color.BLACK)
            val id = View.generateViewId()
            primarySwatchIds.add(id)
            swatch.id = id
            swatch.contentDescription = name
            swatch.isChecked = primaryPref == name
            binding.themePrimarySwatches.addView(swatch)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                swatch.tooltipText = name
            }
        }

        ColorPatchMap.secondaryStyles.forEach { (name, style) ->
            val swatch = layoutInflater.inflate(R.layout.chip_swatch, binding.themeSecondarySwatches, false) as Chip
            val color = context!!.getStyledColor(style, R.attr.colorSecondary)
            swatch.chipBackgroundColor = ColorStateList.valueOf(color)
            swatch.checkedIconTint = ColorStateList.valueOf(Color.BLACK)
            val id = View.generateViewId()
            secondarySwatchIds.add(id)
            swatch.id = id
            swatch.contentDescription = name
            swatch.isChecked = secondaryPref == name
            binding.themeSecondarySwatches.addView(swatch)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                swatch.tooltipText = name
            }
        }

        ColorPatchMap.backgroundStyles.forEach { (name, style) ->
            val swatch = layoutInflater.inflate(R.layout.chip_swatch, binding.themeBackgroundSwatches, false) as Chip
            val color = context!!.getStyledColor(style, android.R.attr.colorBackground)
            swatch.chipBackgroundColor = ColorStateList.valueOf(color)
            val id = View.generateViewId()
            backgroundSwatchIds.add(id)
            swatch.id = id
            swatch.contentDescription = name
            swatch.isChecked = backgroundPref == name
            binding.themeBackgroundSwatches.addView(swatch)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                swatch.tooltipText = name
            }
        }

        setSwatchesEnabled(!binding.themeRandom.isChecked)

        binding.themePrimarySwatches.setOnCheckedChangeListener { group, checkedId ->
            val name = group.findViewById<Chip>(checkedId)?.contentDescription ?: return@setOnCheckedChangeListener
            previewPrimary(name.toString())
            applySameTone(group, primarySwatchIds)
        }

        binding.themeSecondarySwatches.setOnCheckedChangeListener { group, checkedId ->
            val name = group.findViewById<Chip>(checkedId)?.contentDescription ?: return@setOnCheckedChangeListener
            previewSecondary(name.toString())
            applySameTone(group, secondarySwatchIds)
        }

        binding.themeBackgroundSwatches.setOnCheckedChangeListener { group, checkedId ->
            val name = group.findViewById<Chip>(checkedId)?.contentDescription ?: return@setOnCheckedChangeListener
            previewBackground(name.toString())
            applySameTone(group, backgroundSwatchIds)
        }

        binding.themeSameTone.setOnCheckedChangeListener { compoundButton, checked ->
            applySameTone(binding.themePrimarySwatches, primarySwatchIds)
        }

        binding.themeRandom.setOnCheckedChangeListener { compoundButton, checked ->
            setSwatchesEnabled(!checked)
        }

        binding.themeDone.setOnClickListener {
            if ((activity as Main).billingViewModel.proStatus.value == true) {
                saveTheme()
                if (!binding.themeRandom.isChecked)
                    context!!.sendBroadcast(Intent(NLService.iTHEME_CHANGED))
                parentFragmentManager.popBackStack()
                activity!!.recreate()
            } else {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frame, BillingFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onDestroyView() {
        val act = activity as Main
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
        Stuff.setTitle(activity, getString(R.string.pref_themes))
    }

    private fun applySameTone(group: ChipGroup, idList: List<Int>) {
        if (binding.themeSameTone.isChecked) {
            val id = group.checkedChipId
            val idx = idList.indexOf(id)

            primarySwatchIds.elementAtOrNull(idx)?.let {
                binding.themePrimarySwatches.check(it)
            }
            secondarySwatchIds.elementAtOrNull(idx)?.let {
                binding.themeSecondarySwatches.check(it)
            }
            backgroundSwatchIds.elementAtOrNull(idx)?.let {
                binding.themeBackgroundSwatches.check(it)
            }
        }
    }

    private fun previewPrimary(name: String) {
        val styleId = ColorPatchMap.primaryStyles[name]!!
        val color = context!!.getStyledColor(styleId, R.attr.colorPrimary)
        binding.themeDone.backgroundTintList = ColorStateList.valueOf(color)
        val act = activity as Main
        act.binding.coordinatorMain.toolbar.setTitleTextColor(color)
        act.binding.coordinatorMain.toolbar.setArrowColors(
            color,
            Color.TRANSPARENT
        )
    }

    private fun previewSecondary(name: String) {
        val styleId = ColorPatchMap.secondaryStyles[name]!!
        val color = context!!.getStyledColor(styleId, R.attr.colorSecondary)
        binding.themePrimaryHeader.setTextColor(color)
        binding.themeSecondaryHeader.setTextColor(color)
        binding.themeBackgroundHeader.setTextColor(color)
        binding.themeSameTone.buttonTintList = ColorStateList.valueOf(color)
        binding.themeRandom.buttonTintList = ColorStateList.valueOf(color)
    }

    private fun previewBackground(name: String) {
        val styleId = ColorPatchMap.backgroundStyles[name]!!
        val color = context!!.getStyledColor(styleId, android.R.attr.colorBackground)
        binding.root.background = ColorDrawable(color)
        val act = activity as Main
        act.window.navigationBarColor = color
        act.binding.coordinatorMain.ctl.contentScrim = ColorDrawable(color)
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
            binding.themeSecondarySwatches,
            binding.themeBackgroundSwatches,
        )
    }

    private fun getThemeName(group: ChipGroup): String {
        val id = group.checkedChipId
        return group.findViewById<Chip>(id).contentDescription.toString()
    }

    private fun saveTheme() {
        val pref = MultiPreferences(context!!)
        pref.putString(Stuff.PREF_THEME_PRIMARY, getThemeName(binding.themePrimarySwatches))
        pref.putString(Stuff.PREF_THEME_SECONDARY, getThemeName(binding.themeSecondarySwatches))
        pref.putString(Stuff.PREF_THEME_BACKGROUND, getThemeName(binding.themeBackgroundSwatches))
        pref.putBoolean(Stuff.PREF_THEME_RANDOM, binding.themeRandom.isChecked)
        pref.putBoolean(Stuff.PREF_THEME_SAME_TONE, binding.themeSameTone.isChecked)
    }
}