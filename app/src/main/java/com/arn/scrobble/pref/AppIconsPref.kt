package com.arn.scrobble.pref

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Scale
import com.arn.scrobble.R
import com.arn.scrobble.databinding.PrefAppIconsBinding
import com.arn.scrobble.ui.PackageName
import com.arn.scrobble.ui.UiUtils.dp
import com.google.android.material.imageview.ShapeableImageView


/**
 * Created by arn on 09/09/2017.
 */

class AppIconsPref(context: Context, attrs: AttributeSet?, defAttrs: Int, defStyle: Int) :
    Preference(context, attrs, defAttrs, defStyle) {
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : this(
        context,
        attrs,
        defStyle,
        0
    )

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        TypedArrayUtils.getAttr(
            context, androidx.preference.R.attr.preferenceStyle,
            android.R.attr.preferenceStyle
        )
    )

    constructor(context: Context) : this(context, null)

    init {
        layoutResource = R.layout.pref_app_icons
//        widgetLayoutResource = R.layout.pref_app_icons_widget
    }

    private val wPx = 24.dp
    private val maxIcons = 14
    private var prevPackageNames = setOf<String>()

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.isDividerAllowedAbove = false
        val binding = PrefAppIconsBinding.bind(holder.itemView)
        val packageNames = sharedPreferences!!.getStringSet(key, setOf())!!

        if (packageNames != prevPackageNames || binding.appIconsContainer.childCount == 0) {
            binding.appIconsContainer.removeAllViews()

            packageNames
                .take(maxIcons)
                .map {
                    ImageRequest.Builder(context)
                        .data(PackageName(it))
                        .scale(Scale.FIT)
                        .target(
                            onSuccess = { drawable ->
                                ShapeableImageView(context).apply {
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                    layoutParams = LinearLayout.LayoutParams(wPx, wPx)
                                    val padding = wPx / 8
                                    setPadding(padding, padding, padding, padding)
                                    setImageDrawable(drawable)
                                    binding.appIconsContainer.addView(this)
                                }
                            },
                        ).build()
                }.forEach {
                    context.imageLoader.enqueue(it)
                }
            prevPackageNames = packageNames
        }
    }
}