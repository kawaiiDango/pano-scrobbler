package com.arn.scrobble.recents

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Parcel
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IntRange
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil3.dispose
import coil3.load
import coil3.memory.MemoryCache
import coil3.request.error
import coil3.request.placeholder
import com.arn.scrobble.R
import com.arn.scrobble.ReviewPrompter
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.api.file.FileScrobblable
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp600
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.billing.BillingViewModel
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.databinding.ContentMainBinding
import com.arn.scrobble.databinding.ContentScrobblesBinding
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.main.App
import com.arn.scrobble.main.FabData
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.pending.PendingScrobblesWorker
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.FocusChangeListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.ItemLongClickListener
import com.arn.scrobble.ui.MusicEntryImageReq
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.arn.scrobble.ui.createSkeletonWithFade
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.putData
import com.arn.scrobble.utils.Stuff.putSingle
import com.arn.scrobble.utils.UiUtils
import com.arn.scrobble.utils.UiUtils.collectLatestLifecycleFlow
import com.arn.scrobble.utils.UiUtils.expandToHeroIfNeeded
import com.arn.scrobble.utils.UiUtils.memoryCacheKey
import com.arn.scrobble.utils.UiUtils.scrollToTopOnInsertToTop
import com.arn.scrobble.utils.UiUtils.setProgressCircleColors
import com.arn.scrobble.utils.UiUtils.setTitle
import com.arn.scrobble.utils.UiUtils.setupInsets
import com.arn.scrobble.utils.UiUtils.showWithIcons
import com.arn.scrobble.utils.UiUtils.toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Objects


/**
 * Created by arn on 09/07/2017.
 */

class ScrobblesFragment : Fragment(), ItemClickListener<Any>, ScrobblesAdapter.SetHeroTrigger {
    private lateinit var adapter: ScrobblesAdapter
    private val prefs = App.prefs
    private var timedRefreshJob: Job? = null
    private lateinit var coordinatorBinding: ContentMainBinding
    private var _binding: ContentScrobblesBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel by viewModels<TracksVM>()
    private val billingViewModel by activityViewModels<BillingViewModel>()
    private val activityViewModel by activityViewModels<MainNotifierViewModel>()
    private var animSet: AnimatorSet? = null
    private val args by navArgs<ScrobblesFragmentArgs>()

    private val focusChangeListener by lazy {
        object : FocusChangeListener {
            //only called when !view.isInTouchMode
            override fun onFocus(view: View, position: Int) {
                if (!view.isInTouchMode) {
                    val pos = IntArray(2)
                    view.getLocationInWindow(pos)

                    if (pos[1] + view.height > coordinatorBinding.coordinator.height && coordinatorBinding.appBar.isExpanded)
                        coordinatorBinding.appBar.setExpanded(false, true)
                }
            }
        }
    }
    private val itemLongClickListener = object : ItemLongClickListener<Any> {
        override fun onItemLongClick(view: View, position: Int, item: Any) {
            val track = item as? Track ?: return
            showTrackInfo(track)
        }
    }
    lateinit var loadMoreListener: EndlessRecyclerViewScrollListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentScrobblesBinding.inflate(inflater, container, false)
        binding.scrobblesList.setupInsets()
        coordinatorBinding = (activity as MainActivity).binding
        return binding.root
    }

    override fun onDestroyView() {
        coordinatorBinding.heroButtonsGroup.children.forEach { it.setOnClickListener(null) }
        coordinatorBinding.heroImg.dispose()
        coordinatorBinding.heroImg.setImageBitmap(null)
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()

        setTitle(0)
        TransitionManager.beginDelayedTransition(
            coordinatorBinding.ctl,
            Fade().addTarget(coordinatorBinding.heroFrame)
        )
        coordinatorBinding.heroFrame.isVisible = true

        if (binding.scrobblesList.adapter != null) {
            if (viewModel.selectedPos > -1 && viewModel.selectedPos < adapter.itemCount) {
                val track = adapter.getItem(viewModel.selectedPos) as? Track
                if (track != null)
                    onSetHero(track, null)
            }
            doNextTimedRefresh()
        }
        activity ?: return
        viewModel.reEmitColors()

        coordinatorBinding.appBar.expandToHeroIfNeeded(true)
    }

    override fun onPause() {
        super.onPause()

        animSet?.end()
        TransitionManager.beginDelayedTransition(
            coordinatorBinding.ctl,
            Fade().addTarget(coordinatorBinding.heroFrame)
        )
        coordinatorBinding.heroFrame.isVisible = false
        val bgColor = MaterialColors.getColor(
            activity,
            android.R.attr.colorBackground,
            null
        )
        coordinatorBinding.sidebarNav.setBackgroundColor(bgColor)
        coordinatorBinding.ctl.setStatusBarScrimColor(bgColor)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val activity = activity as MainActivity? ?: return

        val llm = LinearLayoutManager(requireContext())
        binding.scrobblesList.layoutManager = llm
        adapter = ScrobblesAdapter(
            fragmentBinding = binding,
            findNavController(),
            itemClickListener = this,
            itemLongClickListener = itemLongClickListener,
            focusChangeListener = focusChangeListener,
            setHeroListener = this,
            viewModel = viewModel,
            userIsSelf = activityViewModel.currentUser.isSelf,
        )
        adapter.isShowingAlbums = prefs.showAlbumInRecents
        adapter.isShowingPlayers =
            !viewModel.isShowingLoves && activityViewModel.currentUser.isSelf &&
                    prefs.proStatus && prefs.showScrobbleSources

        animSet = AnimatorSet()

        binding.scrobblesList.addItemDecoration(SimpleHeaderDecoration())
        binding.swipeRefresh.setProgressCircleColors()
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.setInput(
                viewModel.input.value!!.copyCacheBusted(page = 1)
            )
        }
        binding.scrobblesList.adapter = adapter
        binding.scrobblesList.scrollToTopOnInsertToTop()
        (binding.scrobblesList.itemAnimator as DefaultItemAnimator?)?.supportsChangeAnimations =
            false

        loadMoreListener = EndlessRecyclerViewScrollListener(llm) { page ->
            if (page <= viewModel.totalPages)
                viewModel.setInput(
                    viewModel.input.value!!.copy(page = page)
                )
            loadMoreListener.isAllPagesLoaded = page >= viewModel.totalPages
        }
        loadMoreListener.currentPage = viewModel.input.value?.page ?: 1
        adapter.loadMoreListener = loadMoreListener

        binding.scrobblesList.addOnScrollListener(loadMoreListener)

        val skeleton = binding.scrobblesList.createSkeletonWithFade(
            listItemLayoutResId = R.layout.list_item_recents_skeleton,
        )

        if (Scrobblables.current is FileScrobblable) {
            val fabData = FabData(
                viewLifecycleOwner,
                R.string.fix_it_action,
                R.drawable.vd_open_in_new,
                {
                    (Scrobblables.current as FileScrobblable).documentFile.uri.let { uri ->
                        val intent = Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, "text/*")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(intent)
                    }
                })

            activityViewModel.setFabData(fabData)
        }

        collectLatestLifecycleFlow(viewModel.pendingScrobbles) {
            adapter.updatePendingScrobbles(it)
            collectPendingScrobblesProgress()
        }

        collectLatestLifecycleFlow(viewModel.pendingLoves) {
            adapter.updatePendingLoves(it)
            collectPendingScrobblesProgress()
        }

        collectLatestLifecycleFlow(viewModel.tracks.filterNotNull()) {
            adapter.populate(it)

            if (it.isEmpty()) {
                binding.empty.isVisible = true
            } else {
                binding.empty.isVisible = false

                loadMoreListener.currentPage = viewModel.input.value?.page ?: 1

                activityViewModel.updateCanIndex()
                doNextTimedRefresh()
            }
        }

        collectLatestLifecycleFlow(viewModel.hasLoaded) {
            loadMoreListener.loading = !it

            if (!it) {
                if (adapter.itemCount == 0) {
                    skeleton.showSkeleton()
                }

                if (!skeleton.isSkeleton() && viewModel.input.value?.page == 1)
                    binding.swipeRefresh.isRefreshing = true

                binding.empty.isVisible = false
            } else {
                skeleton.showOriginal()
                binding.swipeRefresh.isRefreshing = false

                if (viewModel.tracks.value?.isEmpty() == true) {
                    binding.empty.isVisible = true
                }
            }
        }

        collectLatestLifecycleFlow(activityViewModel.editData, Lifecycle.State.RESUMED) {
            viewModel.editTrack(it)
            ReviewPrompter(requireActivity()).showIfNeeded()
        }

        collectLatestLifecycleFlow(viewModel.fileException) {
            if (it == null) {
                binding.fileError.isVisible = false
            } else {
                binding.fileError.isVisible = true
                val errText = it.message + "\n\n" +
                        it.documentFile.lastModified()
                            .takeIf { it > 0 }
                            ?.let {
                                getString(R.string.state_scrobbled) + ": " + Stuff.myRelativeTime(
                                    requireContext(),
                                    it,
                                    true
                                )
                            }

                binding.fileError.text = errText
            }
        }

        binding.scrobblesChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            viewModel.input.value ?: return@setOnCheckedStateChangeListener

            when (checkedId) {
                R.id.recents_chip -> viewModel.setInput(
                    viewModel.input.value!!.copy(
                        type = Stuff.TYPE_TRACKS,
                        page = 1,
                        timePeriod = null,
                    )
                )

                R.id.loves_chip -> viewModel.setInput(
                    viewModel.input.value!!.copy(
                        type = Stuff.TYPE_LOVES,
                        page = 1,
                        timePeriod = null,
                    )
                )
            }
        }

        coordinatorBinding.heroShare.setOnClickListener {
            val track = viewModel.tracks.value?.getOrNull(viewModel.selectedPos)
            if (track is Track) {
                val heart = when {
                    track.userloved == true -> "♥️"
                    track.userHated == true -> "💔"
                    else -> ""
                }

                var shareText = if (activityViewModel.currentUser.isSelf)
                    getString(
                        R.string.recents_share,
                        heart + getString(R.string.artist_title, track.artist.name, track.name),
                        Stuff.myRelativeTime(requireContext(), track.date, true)
                    )
                else
                    getString(
                        R.string.recents_share_username,
                        heart + getString(R.string.artist_title, track.artist.name, track.name),
                        Stuff.myRelativeTime(requireContext(), track.date, true),
                        activityViewModel.currentUser.name
                    )

                if (!billingViewModel.proStatus.value)
                    shareText += "\n\n" + getString(R.string.share_sig)

                val i = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, shareText)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val shareAlbumArt = coordinatorBinding.heroImg.drawable as? BitmapDrawable
                if (shareAlbumArt != null) {
                    val albumArtFile = File(requireContext().cacheDir, "share/album_art.jpg")
                    albumArtFile.parentFile!!.mkdirs()
                    FileOutputStream(albumArtFile).use { fos ->
                        shareAlbumArt.bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                    }

                    val albumArtUri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.arn.scrobble.fileprovider",
                        albumArtFile
                    )

                    i.putExtra(Intent.EXTRA_STREAM, albumArtUri)
                    i.type = "image/jpeg"

                }

                startActivity(Intent.createChooser(i, getString(R.string.share)))
            }
        }

        coordinatorBinding.heroInfo.setOnClickListener {
            val track = viewModel.tracks.value?.getOrNull(viewModel.selectedPos)
            if (track is Track) {
                showTrackInfo(track)
                if (!prefs.longPressLearnt) {
                    requireContext().toast(R.string.info_long_press_hint, Toast.LENGTH_LONG)
                    prefs.longPressLearnt = true
                }
            }
        }

        binding.randomChip.setOnClickListener {
            val arguments = Bundle().apply {
                putInt(
                    Stuff.ARG_TYPE,
                    if (viewModel.isShowingLoves)
                        Stuff.TYPE_LOVES
                    else
                        Stuff.TYPE_TRACKS
                )
            }
            findNavController().navigate(R.id.randomFragment, arguments)
        }

        binding.timeJumpChip.setOnClickListener { v ->
            val anchorTime = viewModel.input.value?.timePeriod?.end ?: System.currentTimeMillis()
            val timePeriodsToIcons =
                TimePeriodsGenerator(
                    activityViewModel.currentUser.registeredTime,
                    anchorTime,
                    requireContext()
                ).recentsTimeJumps

            val popupMenu = PopupMenu(requireContext(), v)
            timePeriodsToIcons.forEachIndexed { index, (timePeriod, iconRes) ->
                popupMenu.menu.add(Menu.NONE, index, Menu.NONE, timePeriod.name)
                    .apply {
                        setIcon(iconRes)
                    }
            }
            val customId = -1
            popupMenu.menu.add(
                Menu.NONE,
                customId,
                Menu.NONE,
                getString(R.string.charts_custom)
            )
                .apply {
                    setIcon(R.drawable.vd_calendar_today)
                }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    customId -> {
                        openCalendar()
                    }

                    else -> {

                        binding.timeJumpChip.isCheckable = true
                        binding.timeJumpChip.isChecked = true
                        binding.scrobblesList.scheduleLayoutAnimation()

                        val timePeriod = timePeriodsToIcons[item.itemId].first
                        viewModel.setInput(
                            viewModel.input.value!!.copy(
                                type = Stuff.TYPE_TRACKS,
                                page = 1,
                                timePeriod = timePeriod,
                            )
                        )
                    }
                }
                true
            }
            popupMenu.showWithIcons(
                iconTintColor = MaterialColors.getColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorSecondary,
                    null
                )
            )
        }

        if (!Stuff.isTv)
            coordinatorBinding.heroButtonsGroup.isVisible = true

        // old value found, could be a uiMode change
        viewModel.paletteColors.value?.setDarkModeFrom(requireContext())

        collectLatestLifecycleFlow(
            viewModel.paletteColors.filterNotNull(),
            Lifecycle.State.RESUMED
        ) { colors ->

            if (skeleton.isSkeleton())
                return@collectLatestLifecycleFlow

            val contentBgFrom = (binding.root.background as ColorDrawable).color
            val tintButtonsFrom =
                (coordinatorBinding.heroButtonsGroup.children.first() as MaterialButton)
                    .iconTint.defaultColor

            val animSetList = mutableListOf<Animator>()

            coordinatorBinding.heroButtonsGroup.children.forEach { button ->
                button as MaterialButton
                animSetList += ValueAnimator.ofArgb(
                    tintButtonsFrom,
                    colors.foreground
                ).apply {
                    addUpdateListener {
                        button.iconTint = ColorStateList.valueOf(it.animatedValue as Int)
                    }
                }
            }

            if (!billingViewModel.proStatus.value || prefs.themeTintBackground) {
                if (UiUtils.isTabletUi)
                    animSetList += ObjectAnimator.ofArgb(
                        activity.binding.sidebarNav,
                        "backgroundColor",
                        contentBgFrom,
                        colors.background
                    )
                animSetList += ObjectAnimator.ofArgb(
                    binding.root,
                    "backgroundColor",
                    contentBgFrom,
                    colors.background
                )

                binding.scrobblesList.children.forEach {
                    val titleTextView = it.findViewById<TextView>(R.id.recents_title)
                    if (titleTextView != null)
                        animSetList += ObjectAnimator.ofArgb(
                            titleTextView,
                            "textColor",
                            tintButtonsFrom,
                            colors.foreground
                        )
                }

                activity.binding.ctl.setStatusBarScrimColor(colors.background)
            } else {
                animSetList += ObjectAnimator.ofArgb(
                    binding.root, "backgroundColor", contentBgFrom,
                    MaterialColors.getColor(context, android.R.attr.colorBackground, null)
                )
            }

            animSet?.apply {
                cancel()
                playTogether(animSetList)
                interpolator = AccelerateDecelerateInterpolator()
                duration = 1000
                start()
            }
        }

        binding.scrobblesChipGroup.isVisible = args.showChips

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.setInput(
                    MusicEntryLoaderInput(
                        type = Stuff.TYPE_TRACKS,
                        page = 1,
                        user = activityViewModel.currentUser,
                        timePeriod = null,
                    ), true
                )
            }
        }
    }

    private fun collectPendingScrobblesProgress() {
        if (!activityViewModel.pendingSubmitAttempted &&
            (viewModel.pendingScrobbles.value.isNotEmpty() || viewModel.pendingLoves.value.isNotEmpty())
            && Stuff.isOnline
        ) {
            activityViewModel.pendingSubmitAttempted = true
            PendingScrobblesWorker.checkAndSchedule(requireContext(), true)

            val wm = WorkManager.getInstance(requireContext())
            var lastSnackbar: Snackbar? = null
            collectLatestLifecycleFlow(
                wm.getWorkInfosForUniqueWorkFlow(PendingScrobblesWorker.NAME)
                    .map { it?.firstOrNull() }
                    .filterNotNull(),
                Lifecycle.State.RESUMED
            ) { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        val progressText =
                            workInfo.progress.getString(PendingScrobblesWorker.PROGRESS_KEY)
                                ?: return@collectLatestLifecycleFlow
                        lastSnackbar = Snackbar.make(
                            coordinatorBinding.root,
                            progressText,
                            Snackbar.LENGTH_SHORT
                        )
                            .setAction(android.R.string.cancel) {
                                PendingScrobblesWorker.cancel(requireContext())
                            }
                        lastSnackbar!!.show()
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        lastSnackbar?.dismiss()
                    }

                    else -> {}
                }
            }
        }
    }

    private fun doNextTimedRefresh() {
        timedRefreshJob?.cancel()
        timedRefreshJob = viewLifecycleOwner.lifecycleScope.launch {
            if (!isResumed)
                return@launch

            if (System.currentTimeMillis() - viewModel.lastRecentsLoadTime < Stuff.RECENTS_REFRESH_INTERVAL)
                delay(Stuff.RECENTS_REFRESH_INTERVAL)

            if (!isResumed)
                return@launch

            val input = viewModel.input.value ?: return@launch
            if (input.page == 1 && !viewModel.isShowingLoves && input.timePeriod == null && input.entry == null)
                viewModel.setInput(
                    viewModel.input.value!!.copyCacheBusted(),
                )
        }
    }

    override fun onSetHero(track: Track, cacheKey: MemoryCache.Key?) {
        _binding ?: return

        val reqData: Any = if (viewModel.isShowingLoves)
            MusicEntryImageReq(track, true)
        else
            (track.webp600 ?: "")

        val errColor = UiUtils.getMatColor(
            coordinatorBinding.heroImg.context,
            Objects.hash(track.artist.name, track.name)
        )
        val errDrawable =
            ContextCompat.getDrawable(requireContext(), R.drawable.vd_wave_simple_filled)!!
                .apply { setTint(errColor) }

        coordinatorBinding.heroImg.load(reqData) {
            placeholderMemoryCacheKey(cacheKey ?: coordinatorBinding.heroImg.memoryCacheKey)
            placeholder(R.drawable.avd_loading)
            error(errDrawable)
            listener(
                onSuccess = { _, result ->
                    // Create the palette on a background thread.
                    Palette.Builder(result.image.asDrawable(resources).toBitmap())
                        .generate { palette ->
                            viewModel.setPaletteColors(
                                PaletteColors(
                                    context ?: return@generate,
                                    palette ?: return@generate
                                )
                            )
                        }
                },
                onError = { imageRequest, errorResult ->
                    val swatch = Palette.Swatch(errColor, 1)
                    val palette = Palette.from(listOf(swatch))
                    viewModel.setPaletteColors(PaletteColors(requireContext(), palette))
                }
            )
        }
    }

    override fun onItemClick(view: View, position: Int, item: Any) {
        val dateFrame = (view.parent as ViewGroup).findViewById<FrameLayout>(R.id.date_frame)
        if (item !is Track) {
            if (view.id == R.id.recents_menu)
                PopupMenuUtils.openPendingPopupMenu(
                    dateFrame,
                    viewLifecycleOwner.lifecycleScope,
                    item
                )
            return
        }
        when (view.id) {
            R.id.recents_menu -> openTrackPopupMenu(dateFrame, item)

            else -> {
                val lastClickedPos = viewModel.selectedPos
                viewModel.selectedPos = position
                adapter.notifyItemChanged(lastClickedPos)
                adapter.notifyItemChanged(viewModel.selectedPos)

                if (!coordinatorBinding.appBar.isExpanded) {
//                  recents_list.smoothScrollToPosition(position) //wont work even with smoothscroller

                    val smoothScroller =
                        object : LinearSmoothScroller(binding.scrobblesList.context) {
                            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?) =
                                super.calculateSpeedPerPixel(displayMetrics) * 3

                            override fun getVerticalSnapPreference() = SNAP_TO_START
                        }
                    smoothScroller.targetPosition = position
                    binding.scrobblesList.layoutManager?.startSmoothScroll(smoothScroller)
                    coordinatorBinding.appBar.setExpanded(true, true)
                }

                if (!view.isInTouchMode)
                    openTrackPopupMenu(dateFrame, item)
            }
        }
    }

    private fun openTrackPopupMenu(anchor: View, track: Track) {
        val popup = PopupMenu(requireContext(), anchor)

        if (activityViewModel.currentUser.isSelf && args.showAllMenuItems) {
            popup.menuInflater.inflate(R.menu.recents_item_menu, popup.menu)
            val loveMenu = popup.menu.findItem(R.id.menu_love)

            if (track.userloved == true) {
                loveMenu.title = getString(R.string.unlove)
                loveMenu.icon =
                    ContextCompat.getDrawable(requireContext(), R.drawable.vd_heart_break_outline)
            }

            if (track.userHated == true) {
                popup.menu.findItem(R.id.menu_hate)?.title = getString(R.string.unhate)
            }

            if (track.date == null)
                popup.menu.removeItem(R.id.menu_delete)

            if (viewModel.isShowingLoves) {
                popup.menu.removeItem(R.id.menu_delete)
                popup.menu.removeItem(R.id.menu_edit)
            }
        } else {
            popup.menuInflater.inflate(R.menu.recents_item_friends_menu, popup.menu)
        }

        val moreMenu: Menu = popup.menu.findItem(R.id.menu_more)?.subMenu ?: popup.menu

        if (Scrobblables.current !is ListenBrainz)
            moreMenu.removeItem(R.id.menu_hate)

        if (anchor.isInTouchMode) {
            moreMenu.removeItem(R.id.menu_info)
            moreMenu.removeItem(R.id.menu_share)
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_love -> loveUnloveHate(
                    (anchor.parent as ViewGroup).findViewById(R.id.recents_img_overlay),
                    track,
                    if (track.userloved == true) 0 else 1
                )

                R.id.menu_hate -> loveUnloveHate(
                    (anchor.parent as ViewGroup).findViewById(R.id.recents_img_overlay),
                    track,
                    if (track.userHated == true) 0 else -1
                )

                R.id.menu_edit -> PopupMenuUtils.editScrobble(findNavController(), track)
                R.id.menu_delete -> PopupMenuUtils.deleteScrobble(
                    findNavController(),
                    viewModel.viewModelScope,
                    track
                ) { succ ->
                    if (succ)
                        viewModel.removeTrack(track)
                    else
                        requireActivity().toast(R.string.network_error)
                }

                R.id.menu_block_track, R.id.menu_block_album, R.id.menu_block_artist -> {
                    val blockedMetadata = when (menuItem.itemId) {
                        R.id.menu_block_track -> BlockedMetadata(
                            artist = track.artist.name,
                            album = track.album?.name ?: "",
                            track = track.name,
                            skip = true
                        )

                        R.id.menu_block_album -> BlockedMetadata(
                            artist = track.artist.name,
                            album = track.album?.name ?: "",
                            skip = true
                        )

                        R.id.menu_block_artist -> BlockedMetadata(
                            artist = track.artist.name,
                            skip = true
                        )

                        else -> return@setOnMenuItemClickListener false
                    }

                    val args = Bundle().putSingle(blockedMetadata)
                    findNavController().navigate(R.id.blockedMetadataAddDialogFragment, args)
                }

                R.id.menu_play -> {
                    val pkgName = if (track.date != null)
                        viewModel.pkgMap[track.date]
                    else if (track.isNowPlaying)
                        viewModel.pkgMap[0]
                    else
                        null
                    Stuff.launchSearchIntent(track, pkgName)
                }

                R.id.menu_info -> coordinatorBinding.heroInfo.callOnClick()
                R.id.menu_share -> coordinatorBinding.heroShare.callOnClick()
            }
            true
        }

        popup.showWithIcons()
    }

    private fun openCalendar() {
        val time = viewModel.input.value?.timePeriod?.end ?: System.currentTimeMillis()
        val endTime = System.currentTimeMillis()
        val dpd = MaterialDatePicker.Builder
            .datePicker()
            .setTitleText(R.string.time_jump)
            .setCalendarConstraints(
                CalendarConstraints.Builder()
                    .setStart(activityViewModel.currentUser.registeredTime)
                    .setEnd(endTime)
                    .setOpenAt(time)
                    .setValidator(object : CalendarConstraints.DateValidator {
                        override fun describeContents() = 0

                        override fun writeToParcel(p0: Parcel, p1: Int) {}

                        override fun isValid(date: Long) =
                            date in activityViewModel.currentUser.registeredTime..endTime
                    })
                    .build()
            )
            .setSelection(time)
            .build()
        dpd.addOnPositiveButtonClickListener {
            val toTime = Stuff.timeToLocal(it) + (24 * 60 * 60 - 1) * 1000
//                Timber.i("time=" + Date(viewModel.toTime))

            binding.timeJumpChip.isCheckable = true
            binding.timeJumpChip.isChecked = true
            binding.scrobblesList.scheduleLayoutAnimation()

            viewModel.setInput(
                viewModel.input.value!!.copy(
                    page = 1,
                    type = Stuff.TYPE_TRACKS,
                    timePeriod = TimePeriod(-1, toTime)
                )
            )
        }

        dpd.show(parentFragmentManager, null)
    }

    private fun loveUnloveHate(loveIcon: View, track: Track, @IntRange(-1, 1) score: Int) {
        val alphaAnimator = ObjectAnimator.ofFloat(loveIcon, "alpha", 0f)
        val scalexAnimator = ObjectAnimator.ofFloat(loveIcon, "scaleX", 0f)
        val scaleyAnimator = ObjectAnimator.ofFloat(loveIcon, "scaleY", 0f)
        val rotAnimator = ObjectAnimator.ofFloat(loveIcon, "rotation", 0f)

        val isRtl = resources.getBoolean(R.bool.is_rtl)

        if (score == 0) { // was loved or hated
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                ScrobbleEverywhere.loveOrUnlove(track, false)
            }

            alphaAnimator.setFloatValues(0f)
            scalexAnimator.setFloatValues(1f, 2f)
            scaleyAnimator.setFloatValues(1f, 2f)
            val startRot = if (isRtl) -10f else 10f
            val toRot = if (isRtl) 50f else -50f
            rotAnimator.setFloatValues(startRot, toRot)
        } else {
            loveIcon.background = ContextCompat.getDrawable(
                requireContext(),
                if (score == 1)
                    R.drawable.vd_heart_stroked
                else
                    R.drawable.vd_heart_break_stroked
            )

            if (score == 1)
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    ScrobbleEverywhere.loveOrUnlove(track, true)
                }
            else
                viewModel.viewModelScope.launch {
                    (Scrobblables.current as? ListenBrainz)?.hate(track)
                }

            loveIcon.alpha = 0f
            loveIcon.visibility = View.VISIBLE
            alphaAnimator.setFloatValues(0.9f)
            scalexAnimator.setFloatValues(2f, 1f)
            scaleyAnimator.setFloatValues(2f, 1f)
            val startRot = if (isRtl) 50f else -50f
            val toRot = if (isRtl) -10f else 10f
            rotAnimator.setFloatValues(startRot, toRot)
        }
        val aSet = AnimatorSet()
        aSet.playTogether(alphaAnimator, scalexAnimator, scaleyAnimator, rotAnimator)
        aSet.interpolator = OvershootInterpolator()
        aSet.duration = 800
        aSet.start()

        viewModel.editTrack(track.copy(userloved = score == 1, userHated = score == -1))
    }

    private fun showTrackInfo(track: Track) {
        val arguments = Bundle().putData(track).apply {
            val pkgName = if (track.date != null)
                viewModel.pkgMap[track.date]
            else if (track.isNowPlaying)
                viewModel.pkgMap[0]
            else
                null
            putString(Stuff.ARG_PKG, pkgName)
        }
        findNavController().navigate(R.id.infoFragment, arguments)
    }
}
