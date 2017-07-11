package com.pavelsikun.seekbarpreference;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by Pavel Sikun on 21.05.16.
 */

public class SeekBarPreferenceView extends FrameLayout implements View.OnClickListener {

    private PreferenceControllerDelegate controllerDelegate;

    public SeekBarPreferenceView(Context context) {
        super(context);
        init(null);
    }

    public SeekBarPreferenceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SeekBarPreferenceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SeekBarPreferenceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        controllerDelegate = new PreferenceControllerDelegate(getContext(), true);
        controllerDelegate.loadValuesFromXml(attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        View view = inflate(getContext(), R.layout.seekbar_view_layout, this);
        controllerDelegate.onBind(view);
    }

    @Override
    public void onClick(final View v) {
        controllerDelegate.onClick(v);
    }

    @Override
    public void setEnabled(boolean enabled) {
        controllerDelegate.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return controllerDelegate.isEnabled();
    }

    public int getMaxValue() {
        return controllerDelegate.getMaxValue();
    }

    public void setMaxValue(int maxValue) {
        controllerDelegate.setMaxValue(maxValue);
    }

    public String getTitle() {
        return controllerDelegate.getTitle();
    }

    public void setTitle(String title) {
        controllerDelegate.setTitle(title);
    }

    public String getSummary() {
        return controllerDelegate.getSummary();
    }

    public void setSummary(String summary) {
        controllerDelegate.setSummary(summary);
    }

    public int getMinValue() {
     return controllerDelegate.getMinValue();
    }

    public void setMinValue(int minValue) {
        controllerDelegate.setMinValue(minValue);
    }

    public int getInterval() {
        return controllerDelegate.getInterval();
    }

    public void setInterval(int interval) {
        controllerDelegate.setInterval(interval);
    }

    public int getCurrentValue() {
        return controllerDelegate.getCurrentValue();
    }

    public void setCurrentValue(int currentValue) {
        controllerDelegate.setCurrentValue(currentValue);
    }

    public String getMeasurementUnit() {
        return controllerDelegate.getMeasurementUnit();
    }

    public void setMeasurementUnit(String measurementUnit) {
        controllerDelegate.setMeasurementUnit(measurementUnit);
    }

    public void setOnValueSelectedListener(PersistValueListener persistValueListener) {
        controllerDelegate.setPersistValueListener(persistValueListener);
    }

    public boolean isDialogEnabled() {
        return controllerDelegate.isDialogEnabled();
    }

    public void setDialogEnabled(boolean dialogEnabled) {
        controllerDelegate.setDialogEnabled(dialogEnabled);
    }

    public void setDialogStyle(int dialogStyle) {
        controllerDelegate.setDialogStyle(dialogStyle);
    }
}
