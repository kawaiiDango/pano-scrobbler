package com.arn.scrobble.friends

import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isInvisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import coil3.asDrawable
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.databinding.ContentFriendsBinding
import com.arn.scrobble.databinding.GridItemFriendBinding
import com.arn.scrobble.main.App
import com.arn.scrobble.recents.PaletteColors
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.GenericDiffCallback
import com.arn.scrobble.ui.InitialsDrawable
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.LoadMoreGetter
import com.arn.scrobble.utils.Stuff
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Created by arn on 10/07/2017.
 */

class FriendsAdapter(
    private val fragmentBinding: ContentFriendsBinding,
    private val viewModel: FriendsVM,
    private val viewLifecycleOwner: LifecycleOwner
) : ListAdapter<FriendsVM.FriendsItemHolder, FriendsAdapter.VHUser>(
    GenericDiffCallback { o, n -> o.user.name == n.user.name }
), LoadMoreGetter {
    lateinit var itemClickListener: ItemClickListener<FriendsVM.FriendsItemHolder>
    override lateinit var loadMoreListener: EndlessRecyclerViewScrollListener

    private val shapeAppearanceModel by lazy {
        ShapeAppearanceModel.builder(
            fragmentBinding.root.context,
            R.style.roundedCornersBig,
            R.style.roundedCornersBig
        )
            .build()
    }

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        FriendsItemTouchHelper(this, viewModel, viewLifecycleOwner)
            .attachToRecyclerView(fragmentBinding.friendsGrid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHUser {
        val inflater = LayoutInflater.from(parent.context)
        return VHUser(GridItemFriendBinding.inflate(inflater, parent, false))
    }

    fun getViewBindingForPopup(
        context: Context,
        item: FriendsVM.FriendsItemHolder
    ): GridItemFriendBinding {
        val inflater = LayoutInflater.from(context)
        val binding = GridItemFriendBinding.inflate(inflater, fragmentBinding.root, false)

        val holder = VHUser(binding, false)
        holder.setItemData(item)

        if (!App.prefs.demoMode)
            binding.friendsName.text = (item.user.realname.ifEmpty { item.user.name })
        return binding
    }

    override fun onBindViewHolder(holder: VHUser, position: Int) {
        holder.setItemData(getItem(position))
    }

    inner class VHUser(
        private val binding: GridItemFriendBinding,
        private val isRecyclerItem: Boolean = true
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        var isPinned = false
        private var friendsRecentsJob: Job? = null

        init {
            if (isRecyclerItem) {
                itemView.setOnClickListener(this)
                itemView.isFocusable = true
                binding.friendsTrackFrame.background = null
            }
        }

        override fun onClick(view: View) {
            if (isRecyclerItem)
                itemClickListener.call(itemView, bindingAdapterPosition) {
                    getItem(
                        bindingAdapterPosition
                    )
                }
        }

        fun setItemData(item: FriendsVM.FriendsItemHolder) {
            isPinned = item.isPinned

            binding.friendsName.text =
                (item.user.realname.ifEmpty { item.user.name }) +
                        (if (item.isPinned) " 📍" else "")

            if (App.prefs.demoMode)
                binding.friendsName.text = "User ${bindingAdapterPosition + 1}"

            if (item.trackResult == null) {
                binding.friendsTrackFrame.isInvisible = true

                if (bindingAdapterPosition > -1) {
                    friendsRecentsJob?.cancel()
                    friendsRecentsJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(Stuff.FRIENDS_RECENTS_DELAY)
                        viewModel.loadFriendsRecents(item.user.name)
                    }
                }
            } else {
                item.trackResult
                    .onFailure { error ->
                        if (!isRecyclerItem) {
                            binding.friendsTrackFrame.setOnClickListener(null)
                            binding.friendsTrackFrame.isClickable = false
                            binding.friendsTrackFrame.isFocusable = false
                        }

                        binding.friendsSubtitle.text = if (error is ApiException)
                            error.description
                        else
                            error.localizedMessage

                        binding.friendsTitle.text = null
                        binding.friendsDate.text = null

                        binding.friendsMusicIcon.load(R.drawable.vd_error)
                    }
                    .onSuccess { track ->
                        binding.friendsTitle.text = track.name
                        binding.friendsSubtitle.text = track.artist.name
                        binding.friendsDate.text =
                            Stuff.myRelativeTime(itemView.context, track.date)

                        binding.friendsMusicIcon.load(
                            if (track.isNowPlaying)
                                R.drawable.avd_now_playing
                            else
                                R.drawable.vd_music_circle
                        )

                        if (!isRecyclerItem) {
                            binding.friendsTrackFrame.setOnClickListener {
                                Stuff.launchSearchIntent(track, null)
                            }
                            binding.friendsTrackFrame.isClickable = true
                            binding.friendsTrackFrame.isFocusable = true
                        }
                    }

                if (binding.friendsTrackFrame.isInvisible) {
                    val transition = AutoTransition().apply {
                        addTarget(binding.friendsTrackFrame)
                    }
                    TransitionManager.beginDelayedTransition(binding.root, transition)
                    binding.friendsTrackFrame.isInvisible = false
                }
            }

            val userImgUrl = item.user.largeImage

            if (userImgUrl != binding.friendsPic.tag) {
                binding.friendsPic.tag = userImgUrl
                val bgGray = ContextCompat.getColor(itemView.context, R.color.background_gray)
                val wasCached = viewModel.urlToPaletteMap[userImgUrl] != null
                val color = if (wasCached) {
                    viewModel.urlToPaletteMap[userImgUrl]!!.apply {
                        setDarkModeFrom(itemView.context)
                    }.background
                } else {
                    bgGray
                }
                val bg = itemView.background
                if (bg == null)
                    itemView.background = MaterialShapeDrawable(shapeAppearanceModel).apply {
                        setTint(color)
                    }
                else if (bg is MaterialShapeDrawable) {
                    bg.setTint(color)
                }

                binding.friendsPic
                    .load(userImgUrl) {
                        placeholder(R.drawable.avd_loading)
                        error(InitialsDrawable(itemView.context, item.user))
                        if (!wasCached) {
                            listener(
                                onSuccess = { _, result ->
                                    // Create the palette on a background thread.
                                    Palette.Builder(
                                        result.image
                                            .asDrawable(itemView.context.resources)
                                            .toBitmap()
                                    )
                                        .generate { palette ->
                                            palette ?: return@generate

                                            val paletteColors =
                                                PaletteColors(itemView.context, palette)
                                            val anim = ValueAnimator.ofArgb(
                                                bgGray,
                                                paletteColors.background
                                            )
                                            anim.addUpdateListener {
                                                val bg = itemView.background
                                                if (bg is MaterialShapeDrawable) {
                                                    bg.setTint(it.animatedValue as Int)
                                                }
                                            }

                                            anim.duration = 350
                                            anim.interpolator = AccelerateInterpolator()
                                            anim.start()
                                            viewModel.urlToPaletteMap[userImgUrl] = paletteColors
                                        }
                                }
                            )
                        }
                    }
            }
        }
    }
}