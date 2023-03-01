package com.arn.scrobble.ui

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner

class FabData(
    val lifecycleOwner: LifecycleOwner,
    @StringRes val stringRes: Int,
    @DrawableRes val iconRes: Int,
    val clickListener: View.OnClickListener,
    val longClickListener: View.OnLongClickListener? = null,
)