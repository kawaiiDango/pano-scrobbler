## MaterialSeekBarPreference

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-MaterialSeekBarPreference-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/1756)

As far as I checked, there are no cool implementations of SeekBarPreference. So I decided to make one. Works on API-v7+

<img src="https://raw.githubusercontent.com/MrBIMC/MaterialSeekBarPreference/master/ART/screen_4.jpg" width="255">
<img src="https://raw.githubusercontent.com/MrBIMC/MaterialSeekBarPreference/master/ART/screen_2.jpg" width="255">
<img src="https://raw.githubusercontent.com/MrBIMC/MaterialSeekBarPreference/master/ART/screen_3.jpg" width="255">

#Usage

Add this to your module dependencies:
```groovy
    compile 'com.pavelsikun:material-seekbar-preference:2.3.0+'
````

Reference namespace on top of your layout file:
```xml
    xmlns:sample="http://schemas.android.com/apk/res-auto">
````

Now you can use this view in your preferences layout, just like any other normal preference(API-v11+).
```xml
    <com.pavelsikun.seekbarpreference.SeekBarPreference
        android:key="your_pref_key"
        android:title="SeekbarPreference 2"
        android:summary="Some summary"
        android:enabled="false"
        android:defaultValue="5000"

        sample:msbp_minValue="100"
        sample:msbp_maxValue="10000"
        sample:msbp_interval="200"
        sample:msbp_measurementUnit="%"
        sample:msbp_dialogEnabled="false"/>
````

If you have to support API-v7+, this lib provides also SeekBarPreferenceCompat that works with preference-v7.
```xml
    <com.pavelsikun.seekbarpreference.SeekBarPreferenceCompat
        android:key="your_pref_key"
        android:title="SeekbarPreference 2"
        android:summary="Some summary"
        android:enabled="false"
        android:defaultValue="5000"

        sample:msbp_minValue="100"
        sample:msbp_maxValue="10000"
        sample:msbp_interval="200"
        sample:msbp_measurementUnit="%"
        sample:msbp_dialogEnabled="false"/>
````

Or use MaterialSeekBarView if you prefer to use views instead of preferences(works on v7+):
```xml
    <com.pavelsikun.seekbarpreference.SeekBarPreferenceView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"

        app:msbp_interval="200"
        app:msbp_maxValue="0"
        app:msbp_measurementUnit="bananas"
        app:msbp_minValue="-2000"
        sample:msbp_dialogEnabled="false"

        app:msbp_view_title="SeekBarPreferenceView Example"
        app:msbp_view_summary="As you can see, view uses a bit different xml-attributes for some things"
        app:msbp_view_enabled="false"
        app:msbp_view_defaultValue="0" />
```

Either of way, View/Preference provides next methods to modify and manage it from Java:
```java
    public int getMaxValue();
    public void setMaxValue(int maxValue);

    public int getMinValue();
    public void setMinValue(int minValue);

    public String getTitle();
    public void setTitle(String title);

    public String getSummary();
    public void setSummary(String summary);

    public boolean isEnabled();
    public void setEnabled(boolean enabled);

    public int getInterval();
    public void setInterval(int interval);

    public int getCurrentValue();
    public void setCurrentValue(int currentValue);

    public String getMeasurementUnit();
    public void setMeasurementUnit(String measurementUnit);

    public void setDialogEnabled(boolean dialogEnabled);

    public void setDialogStyle(int dialogStyle);

    // AND for view-only(at least for now), there's a way to get a callback whenever value changes:
    public void setOnValueSelectedListener(PersistValueListener onValuePersisted);
```

As you can see, lib provides 4 universal custom attributes(msbp_minValue, msbp_maxValue, msbp_interval and msbp_measurementUnit).

There are also 4 additional attributes for view bacause it can't use corresponding ones from "android:" (msbp_view_title, msbp_view_summary, msbp_view_enabled and msbp_defaultValue)

Use them to define look and desired behavior.

Prefixes used to avoid attribute collisions with other libs.

# Collaborators
I'd really want to thank:

* [krage](https://github.com/krage) for adding support for referenced resources.
* [NitroG42](https://github.com/NitroG42) for pointing out to attribute collisions.
* [Dmytro Karataiev](https://github.com/dmytroKarataiev) for a fix for defaultValue.
* [Mehmet Akif Tütüncü](https://github.com/mehmetakiftutuncu) for adding support to disable customInputDialog.

#Licence
Lib is licenced under *Apache2 licence*, so you can do whatever you want with it.
I'd highly recommend to push changes back to make it cooler :D

