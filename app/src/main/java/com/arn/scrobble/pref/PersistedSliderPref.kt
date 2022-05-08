package com.arn.scrobble.pref

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.preference.R
import com.arn.scrobble.Stuff
import com.google.android.material.slider.Slider
import java.text.NumberFormat


/**
 * Created by arn on 09/09/2017.
 */

class PersistedSliderPref(context: Context, attrs: AttributeSet?, defAttrs: Int, defStyle: Int) :
    Preference(context, attrs, defAttrs, defStyle), Slider.OnSliderTouchListener,
    Slider.OnChangeListener {
    constructor(context: Context, attrs: AttributeSet?, defAttrs: Int) : this(
        context,
        attrs,
        defAttrs,
        0
    )

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.seekBarPreferenceStyle
    )

    constructor(context: Context) : this(context, null)

    private val mMin: Int
    private val mMax: Int
    private val mSeekBarIncrement: Int
    private var value: Int

    private val mSeekBarKeyListener =
        View.OnKeyListener { itemView, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) {
                return@OnKeyListener false
            }

            // We don't want to propagate the click keys down to the SeekBar view since it will
            // create the ripple effect for the thumb.
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                return@OnKeyListener false
            }

            val slider = itemView.findViewById<Slider>(R.id.seekbar) ?: return@OnKeyListener false

            slider.onKeyDown(keyCode, event)
        }

    init {
        layoutResource = com.arn.scrobble.R.layout.pref_slider
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.SeekBarPreference, defAttrs, defStyle
        )

        mMin = a.getInt(R.styleable.SeekBarPreference_min, 0)
        mMax = a.getInt(R.styleable.SeekBarPreference_android_max, 100)
        mSeekBarIncrement = a.getInt(R.styleable.SeekBarPreference_seekBarIncrement, 0)
        value = mMin
        a.recycle()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.isDividerAllowedAbove = false
        holder.itemView.setOnKeyListener(mSeekBarKeyListener)

        val slider = holder.itemView.findViewById<Slider>(R.id.seekbar)
        slider.valueFrom = mMin.toFloat()
        slider.valueTo = mMax.toFloat()
        if (mSeekBarIncrement > 0)
            slider.stepSize = mSeekBarIncrement.toFloat()

        setValue(slider)

        slider.clearOnSliderTouchListeners()
        slider.addOnSliderTouchListener(this)
        slider.addOnChangeListener(this)

        slider.setLabelFormatter(::getFormattedValue)
        slider.isEnabled = isEnabled
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getInt(index, mMin)
    }

    private fun setValue(slider: Slider) {
        slider.value = value.toFloat()
        ((slider.parent as ViewGroup).getChildAt(1) as TextView).apply {
            text = getFormattedValue(slider.value)
            visibility = View.VISIBLE
        }
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        // conform value to avoid a crash
        var tmpValue = getPersistedInt((defaultValue as? Int) ?: mMin)
            .coerceIn(mMin, mMax)

        if (mSeekBarIncrement > 0) {
            tmpValue = (tmpValue / mSeekBarIncrement) * mSeekBarIncrement
        }

        value = tmpValue
    }

    override fun onStartTrackingTouch(slider: Slider) {
        ((slider.parent as ViewGroup).getChildAt(1) as TextView).visibility = View.INVISIBLE
    }

    override fun onStopTrackingTouch(slider: Slider) {
        this.value = slider.value.toInt()
        setValue(slider)
    }

    override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
        this.value = slider.value.toInt()
        ((slider.parent as ViewGroup).getChildAt(1) as TextView).apply {
            if (visibility == View.VISIBLE)
                text = getFormattedValue(value)
        }
        persistInt(this.value)
    }

    private fun getFormattedValue(floatValue: Float): String {
        val value = floatValue.toInt()
        val suffix = key?.split("_")?.lastOrNull()
        return when (suffix) {
            "per",
            "percent" ->
                "$value%"
            "secs",
            "time" ->
                Stuff.humanReadableDuration(value)
            else ->
                NumberFormat.getInstance().format(value)
        }
    }

    // No need to save instance state if it is persistent

}