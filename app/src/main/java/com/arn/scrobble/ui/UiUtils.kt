package com.arn.scrobble.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.SearchManager
import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.hardware.input.InputManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import coil.request.SuccessResult
import coil.result
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.MainActivity
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.pref.MainPrefs
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import de.umass.lastfm.Track
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object UiUtils {

    private var _hasMouse: Boolean? = null
    private var _isTv: Boolean? = null

    val Int.dp
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    val Int.sp
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, this.toFloat(),
            Resources.getSystem().displayMetrics
        ).toInt()

    fun TextView.setTextAndAnimate(@StringRes stringRes: Int) =
        setTextAndAnimate(context.getString(stringRes))

    fun TextView.setTextAndAnimate(text: String) {
        val oldText = this.text.toString()
        if (oldText == text) return
        TransitionManager.beginDelayedTransition(parent as ViewGroup)
        this.text = text
    }

    fun <T> RecyclerView.Adapter<*>.autoNotify(
        oldList: List<T>,
        newList: List<T>,
        detectMoves: Boolean = false,
        compareContents: (T, T) -> Boolean = { old, new -> old == new },
        compare: (T, T) -> Boolean,
    ) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                compare(oldList[oldItemPosition], newList[newItemPosition])

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                compareContents(oldList[oldItemPosition], newList[newItemPosition])

            override fun getOldListSize() = oldList.size
            override fun getNewListSize() = newList.size
        }, detectMoves)
        diff.dispatchUpdatesTo(this)
    }


    fun View.startFadeLoop() {
        clearAnimation()
        val anim = AlphaAnimation(0.5f, 0.9f).apply {
            duration = 500
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
            interpolator = DecelerateInterpolator()
        }
        startAnimation(anim)
    }

    val ImageView.memoryCacheKey
        get() = (this.result as? SuccessResult)?.memoryCacheKey

    fun Fragment.hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
    }

    fun Fragment.showKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.showSoftInput(view, 0)
    }

    fun Context.attrToThemeId(@AttrRes attributeResId: Int): Int {
        val typedValue = TypedValue()
        if (theme.resolveAttribute(attributeResId, typedValue, true)) {
            return typedValue.data
        }
        throw IllegalArgumentException(resources.getResourceName(attributeResId))
    }

    fun Context.getTintedDrawable(@DrawableRes drawableRes: Int, hash: Int) =
        ContextCompat.getDrawable(this, drawableRes)!!.apply {
            setTint(getMatColor(this@getTintedDrawable, hash))
        }

    fun RecyclerView.ViewHolder.setDragAlpha(dragging: Boolean) {
        itemView.animate().alpha(
            if (dragging) 0.5f
            else 1f
        ).setDuration(150).start()
    }

    fun BottomSheetDialogFragment.scheduleTransition() {
        val viewParent = view?.parent as? ViewGroup ?: return
        val lastRunTime = viewParent.getTag(R.id.time) as? Int ?: 0
        val now = System.currentTimeMillis()
        if (now - lastRunTime < 500) {
            return
        }
        viewParent.setTag(R.id.time, now)

        TransitionManager.beginDelayedTransition(
            viewParent,
            AutoTransition()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .setInterpolator(DecelerateInterpolator())
        )
    }

    fun Snackbar.focusOnTv(): Snackbar {
        if (context.isTv) {
            addCallback(object : Snackbar.Callback() {
                override fun onShown(sb: Snackbar?) {
                    view.postDelayed({
                        view.findViewById<View>(com.google.android.material.R.id.snackbar_action)
                            .requestFocus()
                    }, 200)
                }
            })
        }
        return this
    }

    /**
     * Adds an extra action button to this snackbar.
     * [aLayoutId] must be a layout with a Button as root element.
     * [aLabel] defines new button label string.
     * [aListener] handles our new button click event.
     */
    fun Snackbar.addAction(
        @LayoutRes aLayoutId: Int,
        @StringRes aLabel: Int,
        aListener: View.OnClickListener?
    ): Snackbar {
        addAction(aLayoutId, context.getString(aLabel), aListener)
        return this
    }

    /**
     * Adds an extra action button to this snackbar.
     * [aLayoutId] must be a layout with a Button as root element.
     * [aLabel] defines new button label string.
     * [aListener] handles our new button click event.
     */
    fun Snackbar.addAction(
        @LayoutRes aLayoutId: Int,
        aLabel: String,
        aListener: View.OnClickListener?
    ): Snackbar {
        // Add our button
        val button = LayoutInflater.from(view.context).inflate(aLayoutId, null) as Button
        // Using our special knowledge of the snackbar action button id we can hook our extra button next to it
        view.findViewById<Button>(R.id.snackbar_action).let {
            // Copy layout
            button.layoutParams = it.layoutParams
            // Copy colors
            (button as? Button)?.setTextColor(it.textColors)
            (it.parent as? ViewGroup)?.addView(button)
        }
        button.text = aLabel
        /** Ideally we should use [Snackbar.dispatchDismiss] instead of [Snackbar.dismiss] though that should do for now */
        //extraView.setOnClickListener {this.dispatchDismiss(BaseCallback.DISMISS_EVENT_ACTION); aListener?.onClick(it)}
        button.setOnClickListener { this.dismiss(); aListener?.onClick(it) }
        return this
    }

    fun RecyclerView.mySmoothScrollToPosition(
        position: Int,
        padding: Int = 40.dp,
        animate: Boolean = true
    ) {

        val smoothScroller by lazy {
            object : LinearSmoothScroller(context) {
                override fun getHorizontalSnapPreference() = SNAP_TO_ANY
                override fun getVerticalSnapPreference() = SNAP_TO_ANY

                override fun calculateTimeForScrolling(dx: Int): Int {
                    return super.calculateTimeForScrolling(dx).coerceAtLeast(100)
                    // at least 100ms. Looks instant otherwise for some reason
                }

                override fun calculateDtToFit(
                    viewStart: Int,
                    viewEnd: Int,
                    boxStart: Int,
                    boxEnd: Int,
                    snapPreference: Int
                ): Int {

                    val dtStart = boxStart + padding - viewStart
                    if (dtStart > 0) {
                        return dtStart
                    }
                    val dtEnd = boxEnd - padding - viewEnd
                    if (dtEnd < 0) {
                        return dtEnd
                    }
                    return 0
                }
            }
        }

        smoothScroller.targetPosition = position

        if (animate) {
            layoutManager!!.startSmoothScroll(smoothScroller)
        } else {
            // scroll without animation
            // https://stackoverflow.com/a/51233011/1067596

            doOnNextLayout {
                scrollToPosition(position)
            }
        }
    }

    fun PopupMenu.showWithIcons(iconTintColor: Int? = null) {
        (menu as? MenuBuilder)?.showIcons(iconTintColor)
        show()
    }

    @SuppressLint("RestrictedApi")
    fun MenuBuilder.showIcons(iconTintColor: Int? = null) {
        setOptionalIconsVisible(true)
        visibleItems.forEach { item ->
            val iconMarginPx =
                context.resources.getDimension(R.dimen.popup_menu_icon_padding).toInt()
            if (item.icon != null) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    item.icon = InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0)
                } else {
                    item.icon =
                        object : InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0) {
                            override fun getIntrinsicWidth() =
                                intrinsicHeight + iconMarginPx + iconMarginPx
                        }
                }

                if (iconTintColor != null)
                    item.icon.setTint(iconTintColor)
            }
        }
    }

    fun capMaxSatLum(rgb: Int, maxSat: Float, maxLum: Float): Int {
        val hsl = floatArrayOf(0f, 0f, 0f)
        ColorUtils.RGBToHSL(
            Color.red(rgb),
            Color.green(rgb),
            Color.blue(rgb),
            hsl
        )
        hsl[1] = min(hsl[1], maxSat)
        hsl[2] = min(hsl[2], maxLum)
        return ColorUtils.HSLToColor(hsl)
    }

    fun capMinSatLum(rgb: Int, minSat: Float, maxSat: Float, minLum: Float): Int {
        val hsl = floatArrayOf(0f, 0f, 0f)
        ColorUtils.RGBToHSL(
            Color.red(rgb),
            Color.green(rgb),
            Color.blue(rgb),
            hsl
        )
        hsl[1] = hsl[1].coerceIn(minSat, maxSat)
        hsl[2] = max(hsl[2], minLum)
        return ColorUtils.HSLToColor(hsl)
    }

    fun isDark(color: Int): Boolean {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    fun FragmentManager.dismissAllDialogFragments() {
        for (fragment in fragments) {
            if (fragment is DialogFragment) {
                fragment.dismissAllowingStateLoss()
            }
            fragment.childFragmentManager.dismissAllDialogFragments()
        }
    }

    fun FragmentManager.popBackStackTill(n: Int) {
        while (backStackEntryCount > n) {
            popBackStackImmediate()
        }
    }

    fun Context.openInBrowser(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // prevent infinite loop
            if (MainPrefs(this).lastfmLinksEnabled) {
                browserIntent.`package` = Stuff.getDefaultBrowserPackage(packageManager)
            }

            startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.no_browser)
        }
    }

    fun nowPlayingAnim(np: ImageView, isNowPlaying: Boolean) {
        if (isNowPlaying) {
            np.visibility = View.VISIBLE
            var anim = np.drawable
            if (anim !is AnimatedVectorDrawableCompat || anim !is AnimatedVectorDrawable) {
                np.setImageResource(R.drawable.avd_eq)
                anim = np.drawable
            }
            if (anim is AnimatedVectorDrawableCompat? && anim?.isRunning != true) {
                anim?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        if (drawable?.isVisible == true)
                            np.post {
                                (np.drawable as? AnimatedVectorDrawableCompat)?.start()
                            }
                    }
                })
                anim?.start()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && anim is AnimatedVectorDrawable && !anim.isRunning) {
                anim.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        if (drawable?.isVisible == true)
                            (drawable as? AnimatedVectorDrawable)?.start()
                    }
                })
                anim.start()
            }
        } else {
            np.visibility = View.GONE
            np.setImageDrawable(null)
        }
    }

    fun SwipeRefreshLayout.setProgressCircleColors() {
        setColorSchemeColors(
            MaterialColors.getColor(this, R.attr.colorPrimary),
            MaterialColors.getColor(this, R.attr.colorSecondary)
        )
        setProgressBackgroundColorSchemeColor(
            MaterialColors.getColor(
                this,
                R.attr.colorPrimaryContainer
            )
        )
    }

    fun launchSearchIntent(context: Context, track: Track, pkgName: String?) {
        launchSearchIntent(context, track.name, track.artist, pkgName)
    }

    fun launchSearchIntent(context: Context, artist: String, track: String, pkgName: String?) {
        val prefs = MainPrefs(context)

        if (BuildConfig.DEBUG && Stuff.isWindows11 && prefs.songSearchUrl.isNotEmpty()) { // open song urls in windows browser for me
            val searchUrl = prefs.songSearchUrl
                .replace("\$artist", artist)
                .replace("\$title", track)
            context.openInBrowser(searchUrl)
            return
        }

        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist)
            putExtra(MediaStore.EXTRA_MEDIA_TITLE, track)
            putExtra(SearchManager.QUERY, "$artist $track")
            if (pkgName != null && prefs.proStatus && prefs.showScrobbleSources && prefs.searchInSource)
                `package` = pkgName
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            if (pkgName != null) {
                try {
                    intent.`package` = null
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    context.toast(R.string.no_player)
                }
            } else
                context.toast(R.string.no_player)
        }
    }

    fun getColoredTitle(
        context: Context,
        title: String,
        @AttrRes colorAttr: Int = R.attr.colorPrimary
    ) =
        SpannableString(title)
            .apply {
                setSpan(
                    ForegroundColorSpan(
                        MaterialColors.getColor(context, colorAttr, null)
                    ),
                    0, title.length,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }

    fun getMatColor(
        c: Context,
        seed: Int,
        colorWeight: String? = if (c.resources.getBoolean(R.bool.is_dark))
            "200"
        else
            "500"
    ): Int {
        val colorNamesArray = c.resources.getStringArray(R.array.mdcolor_names)
        val index = abs(seed) % colorNamesArray.size
        val colorName = colorNamesArray[index]

        val colorId =
            c.resources.getIdentifier("mdcolor_${colorName}_$colorWeight", "color", c.packageName)
        return ContextCompat.getColor(c, colorId)
    }

    fun Fragment.setTitle(@StringRes strId: Int) {
        val title = if (strId == 0)
            null
        else
            context!!.getString(strId)
        setTitle(title)
    }

    fun Fragment.setTitle(str: String?) {
        if (isDetached || isRemoving)
            return

        val activity = activity as MainActivity
        if (str == null) { // = clear title
            activity.binding.coordinatorMain.toolbar.title = null
//            activity.window.navigationBarColor = lastColorMutedBlack
        } else {
            activity.binding.coordinatorMain.toolbar.title = str
            activity.binding.coordinatorMain.appBar.setExpanded(expanded = false, animate = true)

            val navbarBgAnimator = ValueAnimator.ofArgb(activity.window.navigationBarColor, 0)
            navbarBgAnimator.duration *= 2
            navbarBgAnimator.addUpdateListener {
                activity.window.navigationBarColor = it.animatedValue as Int
            }
            navbarBgAnimator.start()
        }
        activity.window.navigationBarColor =
            MaterialColors.getColor(activity, android.R.attr.colorBackground, null)
        activity.binding.coordinatorMain.ctl.setContentScrimColor(
            MaterialColors.getColor(
                activity,
                android.R.attr.colorBackground,
                null
            )
        )
        activity.binding.coordinatorMain.toolbar.setArrowColors(
            MaterialColors.getColor(
                activity,
                R.attr.colorPrimary,
                null
            ), Color.TRANSPARENT
        )
    }

    fun Toolbar.setArrowColors(fg: Int, bg: Int) {
        for (i in 0..childCount) {
            val child = getChildAt(i)
            if (child is ImageButton) {
                (child.drawable as ShadowDrawerArrowDrawable).setColors(fg, bg)
                break
            }
        }
    }

    fun BottomSheetDialogFragment.expandIfNeeded() {
        val bottomSheetView =
            dialog!!.window!!.decorView.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        if (view?.isInTouchMode == false || context!!.hasMouse)
            BottomSheetBehavior.from(bottomSheetView).state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun CollapsingToolbarLayout.adjustHeight(additionalHeight: Int = 0) {
        val activity = context as MainActivity

        val sHeightPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity.windowManager.currentWindowMetrics
            val insets =
                windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            windowMetrics.bounds.height() - insets.top - insets.bottom
        } else {
            val dm = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(dm)
            dm.heightPixels
        }

        val abHeightPx = activity.resources.getDimension(R.dimen.app_bar_height)
        val targetAbHeight: Int
        val lp = activity.binding.coordinatorMain.ctl.layoutParams
        val margin = 65.dp

        targetAbHeight = if (sHeightPx < abHeightPx + additionalHeight + margin)
            ((sHeightPx - additionalHeight) * 0.6).toInt()
        else
            activity.resources.getDimensionPixelSize(R.dimen.app_bar_height)
        if (targetAbHeight != lp.height) {
            if (!activity.binding.coordinatorMain.appBar.isExpanded) {
                lp.height = targetAbHeight
//                activity.app_bar.setExpanded(false, false)
            } else {
                val start = lp.height
                val anim = ValueAnimator.ofInt(start, targetAbHeight)
                anim.addUpdateListener { valueAnimator ->
                    lp.height = valueAnimator.animatedValue as Int
                    activity.binding.coordinatorMain.ctl.layoutParams = lp
                }
                anim.interpolator = DecelerateInterpolator()
                anim.duration = 300
                anim.start()
            }
        }
    }

    fun Context.toast(@StringRes strRes: Int, len: Int = Toast.LENGTH_SHORT) {
        toast(getString(strRes), len)
    }

    fun Context.toast(text: String, len: Int = Toast.LENGTH_SHORT) {
        try {
            Toast.makeText(this, text, len).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val Context.hasMouse: Boolean
        get() {
            if (_hasMouse == null) {
                val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
                _hasMouse = inputManager.inputDeviceIds.any {
                    val device = inputManager.getInputDevice(it)
                    // for windows 11 wsa
                    device.supportsSource(InputDevice.SOURCE_MOUSE) or
                            device.supportsSource(InputDevice.SOURCE_STYLUS)
                }
            }
            return _hasMouse!!
        }

    val Context.isTv: Boolean
        get() {
            if (_isTv == null) {
                val uiModeManager =
                    getSystemService(AppCompatActivity.UI_MODE_SERVICE) as UiModeManager
                _isTv = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
            }
            return _isTv!!
        }
}