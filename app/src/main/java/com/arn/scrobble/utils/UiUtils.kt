package com.arn.scrobble.utils

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavArgumentBuilder
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.error
import coil3.request.transformations
import coil3.result
import coil3.transform.CircleCropTransformation
import com.arn.scrobble.R
import com.arn.scrobble.databinding.LayoutSnowfallBinding
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.main.DrawerData
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.themes.ColorPatchUtils
import com.arn.scrobble.ui.InitialsDrawable
import com.arn.scrobble.ui.StatefulAppBar
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


object UiUtils {

    var isTabletUi = false
    val Int.dp
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    val Int.sp
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, this.toFloat(),
            Resources.getSystem().displayMetrics
        ).toInt()

    const val DISABLED_ALPHA = 0.5f

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
        ContextCompat.getSystemService(context ?: return, InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
    }

    fun Fragment.showKeyboard(view: View) {
        ContextCompat.getSystemService(context ?: return, InputMethodManager::class.java)
            ?.showSoftInput(view, 0)
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

    fun BottomNavigationView.slide(up: Boolean = true) {
        if (layoutParams is CoordinatorLayout.LayoutParams) {
            val behavior =
                (layoutParams as CoordinatorLayout.LayoutParams).behavior as? HideBottomViewOnScrollBehavior
            if (behavior is HideBottomViewOnScrollBehavior) {
                if (up)
                    behavior.slideUp(this)
                else
                    behavior.slideDown(this)
            }
        }
    }

    fun Context.getDimenFromAttr(@AttrRes dimenAttr: Int): Int {
        val ta = obtainStyledAttributes(intArrayOf(dimenAttr))
        val dimen = ta.getDimensionPixelSize(0, -1)
        ta.recycle()
        return dimen
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

    fun StatefulAppBar.expandToHeroIfNeeded(expand: Boolean) {
        if (getTag(R.id.app_bar_can_change_size) != true)
            return

        updateHeight(expand)

        if (expand && !isExpanded)
            setExpanded(true, true)

        setTag(R.id.app_bar_can_change_size, false)
    }

    @SuppressLint("RestrictedApi")
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
                item.icon = InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0)

                if (iconTintColor != null)
                    item.icon?.setTint(iconTintColor)
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
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(
                color
            )) / 255
        return darkness >= 0.5
    }

    fun invertColor(color: Int) = Color.rgb(
        255 - Color.red(color),
        255 - Color.green(color),
        255 - Color.blue(color),
    )

    fun SwipeRefreshLayout.setProgressCircleColors() {
        setColorSchemeColors(
            MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary),
            MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondary)
        )
        setProgressBackgroundColorSchemeColor(
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorPrimaryContainer
            )
        )
    }

    fun loadSmallUserPic(
        context: Context,
        userCached: UserCached,
        drawerData: DrawerData?,
        onResult: (Drawable) -> Unit
    ) {
        if (Stuff.isInDemoMode) {
            onResult(ContextCompat.getDrawable(context, R.drawable.vd_user)!!)
            return
        }

        val initialsDrawable = InitialsDrawable(context, userCached)
        val profilePicUrl =
            if (userCached.isSelf)
                drawerData?.profilePicUrl
            else
                userCached.largeImage
        val request = ImageRequest.Builder(context)
            .data(profilePicUrl?.ifEmpty { null } ?: initialsDrawable)
            .error(initialsDrawable) // does not apply transformation to error drawable
            .transformations(CircleCropTransformation())
            .target(
                onSuccess = { onResult(it!!.asDrawable(context.resources)) },
                onStart = { onResult(ContextCompat.getDrawable(context, R.drawable.vd_user)!!) },
                onError = { onResult(it!!.asDrawable(context.resources)) }
            )
            .build()
        context.imageLoader.enqueue(request)
    }

    fun getColoredTitle(
        context: Context,
        title: String,
        @AttrRes colorAttr: Int = com.google.android.material.R.attr.colorPrimary
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
            c.resources.getIdentifier(
                "mdcolor_${colorName}_$colorWeight",
                "color",
                c.packageName
            )
        return ContextCompat.getColor(c, colorId)
    }

    fun View.setupInsets(
        marginMode: Boolean = this !is RecyclerView &&
                this !is ScrollView && this !is NestedScrollView,
        addBottomNavHeight: Boolean = true,
        additionalSpaceBottom: Int = 0,
        additionalSpaceSides: Int = 0,
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomInset = if (addBottomNavHeight)
                view.context.resources.getDimension(R.dimen.bottom_nav_height).toInt() +
                        insets.bottom + insets.top
            else
                insets.bottom + insets.top

            if (marginMode) {
                view.updateLayoutParams {
                    if (this is MarginLayoutParams) {
                        leftMargin = insets.left + additionalSpaceSides
                        bottomMargin = bottomInset + additionalSpaceBottom
                        rightMargin = insets.right + additionalSpaceSides
                    }
                }
            } else {
                view.updatePadding(
                    left = insets.left + additionalSpaceSides,
                    bottom = bottomInset + additionalSpaceBottom,
                    right = insets.right + additionalSpaceSides,
                )
            }


            // Return CONSUMED if you don't want want the window insets to keep being
            // passed down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
    }

    fun Fragment.setTitle(@StringRes strId: Int) {
        val title = if (strId == 0)
            null
        else
            requireContext().getString(strId)
        setTitle(title)
    }

    fun Fragment.setTitle(str: String?) {
//        if (isDetached || isRemoving || !isResumed)
//            return

        val activity = activity as? MainActivity ?: return
        val title = str ?: " "

        findNavController().currentDestination?.addArgument(
            Stuff.ARG_TITLE,
            NavArgumentBuilder().apply {
                defaultValue = title
                nullable = true
            }.build()
        )

        if (activity.binding.ctl.title == title)
            return

        fadeToolbarTitle(activity.binding.ctl)
        activity.binding.ctl.title = title
    }

    fun BottomSheetDialogFragment.expandIfNeeded(dialog: Dialog, force: Boolean = false) {
        dialog.setOnShowListener {
            val bottomSheetDialog = it as BottomSheetDialog
            if (Stuff.isTv || isTabletUi || force) {
                val bottomSheetView =
                    bottomSheetDialog.window!!.decorView.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                val behavior = BottomSheetBehavior.from(bottomSheetView)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            WindowCompat.setDecorFitsSystemWindows(bottomSheetDialog.window!!, false)

            // todo remove
//            if (BuildConfig.DEBUG) {
//                viewLifecycleOwner.lifecycleScope.launch {
//                    var prevFocus: View? = null
//                    while (isActive) {
//                        delay(1000)
//                        val currentFocus =
//                            bottomSheetDialog.window!!.currentFocus
//                        if (currentFocus != prevFocus) {
//                            prevFocus = currentFocus
//                            Timber.dLazy { "focus: $currentFocus" }
//                        }
//                    }
//                }
//            }
        }
    }

    fun RecyclerView.scrollToTopOnInsertToTop() {
        val llm = layoutManager as? LinearLayoutManager ?: return
        adapter!!.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val firstVisiblePosition = llm.findFirstCompletelyVisibleItemPosition()
                if (firstVisiblePosition == 0)
                    llm.scrollToPosition(0)
            }
        })
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

    fun EditText.trimmedText() = text.toString().trim()

    fun createNotificationForFgs(context: Context, title: String): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val launchIntent = PendingIntent.getActivity(
            context, 8, intent,
            Stuff.updateCurrentOrImmutable
        )
        return NotificationCompat.Builder(context, Stuff.CHANNEL_NOTI_PENDING)
            .setSmallIcon(R.drawable.vd_noti)
            .setPriority(Notification.PRIORITY_MIN)
            .setContentIntent(launchIntent)
            .apply { color = (ColorPatchUtils.getNotiColor(context) ?: return@apply) }
            .setContentTitle(title)
            .build()
    }

    fun Fragment.setupAxisTransitions(enterAxis: Int, popAxis: Int = enterAxis) {
        enterTransition = MaterialSharedAxis(enterAxis, true)
        returnTransition = MaterialSharedAxis(enterAxis, false)
        exitTransition = MaterialSharedAxis(popAxis, true)
        reenterTransition = MaterialSharedAxis(popAxis, false)
    }

    fun fadeToolbarTitle(ctl: CollapsingToolbarLayout) {
        // fade in title color
        val va = ValueAnimator.ofArgb(
            Color.TRANSPARENT,
            MaterialColors.getColor(ctl, com.google.android.material.R.attr.colorPrimary)
        )
        va.addUpdateListener {
            ctl.setExpandedTitleColor(it.animatedValue as Int)
//            ctl.setCollapsedTitleTextColor(it.animatedValue as Int)
        }
        va.interpolator = FastOutSlowInInterpolator()
        va.start()
    }

    fun <T> Fragment.collectLatestLifecycleFlow(
        flow: Flow<T>,
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        block: suspend (T) -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(lifecycleState) {
                flow.collectLatest(block)
            }
        }
    }

    // gives you both the previous and the new value
    fun <T> Fragment.collectRunningFoldFlow(
        flow: Flow<List<T>>,
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        block: suspend (List<T>, List<T>) -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(lifecycleState) {
                flow.runningFold(
                    initial = null as Pair<List<T>?, List<T>>?,
                    operation = { accumulator, new -> accumulator?.second to new }
                ).filterNotNull()
                    .collectLatest { block(it.first ?: emptyList(), it.second) }
            }
        }
    }

    fun <T> AppCompatActivity.collectLatestLifecycleFlow(
        flow: Flow<T>,
        block: suspend (T) -> Unit
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collectLatest(block)
            }
        }
    }

    fun Bitmap.toIntArray(): IntArray {
        val colors = IntArray(width * height)
        getPixels(colors, 0, width, 0, 0, width, height)
        return colors
    }

    fun applySnowfall(
        anchor: View,
        container: ViewGroup,
        inflater: LayoutInflater,
        coroutineScope: CoroutineScope
    ) {
        val emojiPairs = arrayOf(
            "\u26C4" to "\u2603️",
            "\uD83C\uDF32" to "\uD83C\uDF84",
            "\u2B50" to "\uD83C\uDF1F"
        )
        var currentIdx = 0
        val binding = LayoutSnowfallBinding.inflate(inflater, container, false)

        fun updateEmojis() {
            binding.snowfallText1.text = emojiPairs[currentIdx].first
            binding.snowfallText2.text = emojiPairs[currentIdx].second
        }

        binding.root.layoutParams = anchor.layoutParams

        updateEmojis()

        container.addView(binding.root)

        binding.snowfallText1.setOnClickListener {
            currentIdx = (currentIdx + 1) % emojiPairs.size
            updateEmojis()
        }

        coroutineScope.launch {
            while (isActive) {
                binding.snowfallText2.visibility = View.INVISIBLE
                delay(1000)
                binding.snowfallText2.visibility = View.VISIBLE
                delay(500)
            }
        }
    }

}