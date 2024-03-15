package com.arn.scrobble.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.arn.scrobble.R
import com.arn.scrobble.utils.UiUtils.dp
import com.faltenreich.skeletonlayout.Skeleton
import com.faltenreich.skeletonlayout.SkeletonConfig
import com.faltenreich.skeletonlayout.SkeletonLayout
import com.faltenreich.skeletonlayout.SkeletonStyle
import com.faltenreich.skeletonlayout.createSkeleton
import com.faltenreich.skeletonlayout.mask.SkeletonShimmerDirection

class SkeletonRecyclerViewFade(
    private val recyclerView: RecyclerView,
    private val skeleton: RecyclerView,
    @LayoutRes layoutResId: Int,
    itemCount: Int,
    config: SkeletonConfig
) : Skeleton, SkeletonStyle by config {

    private var skeletonAdapter =
        SkeletonRecyclerViewFadeAdapter(layoutResId, itemCount, config)
    private var showRunnable: Runnable? = null

    init {
        skeleton.adapter = skeletonAdapter
        config.addValueObserver { skeletonAdapter.notifyDataSetChanged() }
    }

    override fun showOriginal() {
        if (showRunnable != null) {
            skeleton.removeCallbacks(showRunnable)
            showRunnable = null
        }

        if (isSkeleton()) {
            val skeletonOut = Fade().apply {
                addTarget(recyclerView)
                addTarget(skeleton)
            }
            TransitionManager.beginDelayedTransition(skeleton.parent as ViewGroup, skeletonOut)
            skeleton.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    // shows skeleton after a delay
    override fun showSkeleton() {
        showRunnable = skeleton.postDelayed(150) {
            if (!isSkeleton()) {
                skeleton.visibility = View.VISIBLE
                recyclerView.visibility = View.INVISIBLE
            }
        }
    }

    override fun isSkeleton() = skeleton.isVisible
}

private class SkeletonRecyclerViewFadeAdapter(
    @LayoutRes private val layoutResId: Int,
    private val itemCount: Int,
    private val config: SkeletonConfig
) : RecyclerView.Adapter<SkeletonRecyclerViewFadeHolder>() {

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SkeletonRecyclerViewFadeHolder {
        val originView = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        val skeleton = originView.createSkeleton(config) as SkeletonLayout
        skeleton.layoutParams = originView.layoutParams
        skeleton.showSkeleton()
        return SkeletonRecyclerViewFadeHolder(skeleton)
    }

    override fun onBindViewHolder(holder: SkeletonRecyclerViewFadeHolder, position: Int) = Unit

    override fun getItemCount() = itemCount
}

private class SkeletonRecyclerViewFadeHolder(itemView: SkeletonLayout) :
    RecyclerView.ViewHolder(itemView)

fun RecyclerView.createSkeletonWithFade(
    @LayoutRes listItemLayoutResId: Int,
    itemCount: Int = 10,
    skeletonConfigRadiusDp: Int = 12,
): Skeleton {
    var parent = parent as ViewGroup
    var params = layoutParams

    if (parent is SwipeRefreshLayout) { // SwipeRefreshLayout can take only one child
        params = parent.layoutParams
        parent = parent.parent as ViewGroup
    }

    val index = parent.indexOfChild(this).coerceAtLeast(0)

    val skeleton = RecyclerView(context).also {
        id = View.generateViewId()
        it.clipToPadding = clipToPadding
        it.isNestedScrollingEnabled = isNestedScrollingEnabled
        val layoutManager = layoutManager

        it.layoutManager = when (layoutManager) {
            is GridLayoutManager -> GridLayoutManager(context, layoutManager.spanCount)
            is LinearLayoutManager -> LinearLayoutManager(
                context,
                layoutManager.orientation,
                layoutManager.reverseLayout
            )

            else -> throw IllegalArgumentException("Unsupported LayoutManager")
        }
        it.visibility = View.GONE
    }

    parent.addView(skeleton, index, params)

    val config = mySkeletonConfig(context, skeletonConfigRadiusDp)

    return SkeletonRecyclerViewFade(this, skeleton, listItemLayoutResId, itemCount, config)
}

class SkeletonViewFade(
    private val original: View,
    private val skeleton: SkeletonLayout,
    config: SkeletonConfig
) : Skeleton, SkeletonStyle by config {

    private var showRunnable: Runnable? = null

    override fun showOriginal() {
        if (showRunnable != null) {
            skeleton.removeCallbacks(showRunnable)
            showRunnable = null
        }

        if (isSkeleton()) {
            val skeletonOut = Fade().apply {
                addTarget(original)
                addTarget(skeleton)
            }
            TransitionManager.beginDelayedTransition(skeleton.parent as ViewGroup, skeletonOut)
            skeleton.visibility = View.GONE
            original.visibility = View.VISIBLE
        }
    }

    // shows skeleton after a delay
    override fun showSkeleton() {
        showRunnable = skeleton.postDelayed(150) {
            if (!isSkeleton()) {
                skeleton.showSkeleton()
                skeleton.visibility = View.VISIBLE
                original.visibility = View.INVISIBLE
            }
        }
    }

    override fun isSkeleton() = skeleton.isVisible
}


fun View.createSkeletonWithFade(
    skeletonView: View,
    radiusDp: Int = 12
): Skeleton {
    val config = mySkeletonConfig(context, radiusDp)

    val skeleton = (skeletonView.createSkeleton(config) as SkeletonLayout).also {
        it.visibility = View.GONE
    }

    return SkeletonViewFade(this, skeleton, config)
}

private fun mySkeletonConfig(context: Context, radiusDp: Int = 12) = SkeletonConfig(
    maskCornerRadius = radiusDp.dp.toFloat(),
    shimmerAngle = 22,
    shimmerColor = ContextCompat.getColor(context, R.color.skeleton_shimmer),
    maskColor = ContextCompat.getColor(context, R.color.skeleton_mask),
    showShimmer = true,
    shimmerDurationInMillis = 1000,
    shimmerDirection = if (context.resources.getBoolean(R.bool.is_rtl))
        SkeletonShimmerDirection.RIGHT_TO_LEFT
    else
        SkeletonShimmerDirection.LEFT_TO_RIGHT
)