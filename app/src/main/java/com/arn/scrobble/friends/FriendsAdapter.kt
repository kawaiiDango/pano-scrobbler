package com.arn.scrobble.friends

import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.arn.scrobble.R
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
        private val clickable: Boolean = true
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        var isPinned = false
        private var friendsRecentsJob: Job? = null

        init {
            if (clickable) {
                itemView.setOnClickListener(this)
                binding.friendsPic.setOnClickListener(this)
                binding.friendsPic.isFocusable = true
            }
        }

        override fun onClick(view: View) {
            if (clickable)
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

            val track = item.track
            if (track?.name != null && track.name != "") {
                binding.friendsTrackLl.visibility = View.VISIBLE
                binding.friendsTitle.text = track.name
                binding.friendsSubtitle.text = track.artist.name
                binding.friendsDate.text = Stuff.myRelativeTime(itemView.context, track.date)

                binding.friendsMusicIcon.load(
                    if (track.isNowPlaying)
                        R.drawable.avd_now_playing
                    else
                        R.drawable.vd_music_circle
                )

                binding.friendsTrackFrame.setOnClickListener {
                    Stuff.launchSearchIntent(track, null)
                }
            } else {
                binding.friendsTrackLl.visibility = View.INVISIBLE
                binding.friendsTrackFrame.setOnClickListener(null)

                binding.friendsMusicIcon.load(R.drawable.vd_music_circle)

                if (bindingAdapterPosition > -1) {
                    friendsRecentsJob?.cancel()
                    friendsRecentsJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(Stuff.FRIENDS_RECENTS_DELAY)
                        viewModel.loadFriendsRecents(item.user.name)
                    }
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
                                        result.drawable.toBitmap()
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