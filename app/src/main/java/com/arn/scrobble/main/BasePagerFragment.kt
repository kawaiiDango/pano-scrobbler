package com.arn.scrobble.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.activityViewModels
import com.arn.scrobble.databinding.ContentPagerBinding
import com.arn.scrobble.ui.OptionsMenuVM
import com.arn.scrobble.utils.NavUtils.setupWithNavUi
import com.arn.scrobble.utils.UiUtils.setupAxisTransitions
import com.google.android.material.transition.MaterialSharedAxis
import kotlin.math.abs


open class BasePagerFragment : Fragment() {

    lateinit var adapter: BasePagerAdapter
    open val optionsMenuRes = 0
    private var _binding: ContentPagerBinding? = null
    val binding
        get() = _binding!!
    val optionsMenuViewModel by activityViewModels<OptionsMenuVM>()
    val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()
    var isReady = false
        private set


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupAxisTransitions(MaterialSharedAxis.Z)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentPagerBinding.inflate(inflater, container, false)
        isReady = true
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        isReady = false
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        //https://stackoverflow.com/questions/12490963/replacing-viewpager-with-fragment-then-navigating-back
        if (!view.isInTouchMode)
            view.requestFocus()

        binding.pager.offscreenPageLimit = adapter.count - 1
        binding.pager.adapter = adapter

        (view.parent as? ViewGroup)?.doOnPreDraw {
            startPostponedEnterTransition()
        }

        setupWithNavUi()

        binding.pager.setPageTransformer(false) { page, position ->
            when {
                position <= -1 -> // [-Infinity,-1) // This page is way off-screen to the left.
                    page.alpha = 0f

                position < 1 && position > -1 ->  // [-1,1]
                    page.alpha = 1 - abs(position)

                else -> // (1,+Infinity] // This page is way off-screen to the right.
                    page.alpha = 0f

            }
        }
    }
}

abstract class BasePagerAdapter(fragment: BasePagerFragment) : FragmentPagerAdapter(
    fragment.childFragmentManager,
    BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
) {
    abstract val tabMetadata: List<TabMetadata>
    override fun getItem(position: Int) = tabMetadata[position].fragment()
    override fun getCount() = tabMetadata.size
}

class TabMetadata(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val fragment: () -> Fragment
)