package com.arn.scrobble

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.arn.scrobble.databinding.ContentRandomBinding
import com.arn.scrobble.info.InfoFragment
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track


/**
 * Created by arn on 06/09/2017.
 */
class RandomFragment: Fragment() {

    private val viewModel by lazy { VMFactory.getVM(this, RandomVM::class.java) }
    private val prefs by lazy { context!!.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE) }
    private var isLoading = false
    private var _binding: ContentRandomBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = android.transition.Fade()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContentRandomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        Stuff.setTitle(activity, R.string.menu_random)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setContainerWidth()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.randomGetLoved.setOnClickListener {
            if (!isLoading) {
                viewModel.loadRandomLove()
                setLoading(true)
            }
        }
        binding.randomGetScrobble.setOnClickListener {
            if (!isLoading) {
                viewModel.loadRandomScrobble()
                setLoading(true)
            }
        }
        binding.randomTrack.recentsMenu.visibility = View.INVISIBLE
        binding.randomTrack.recentsPlaying.visibility = View.INVISIBLE
        (binding.randomTrack.recentsImg.parent as FrameLayout).visibility = View.GONE
        binding.randomTrack.recentsDate.setTextColor(Color.WHITE)
        binding.randomTrack.root.setBackgroundResource(R.drawable.layer_random_track)
        binding.randomTrack.root.setPaddingRelative(0, resources.getDimension(R.dimen.gradient_container_top_padding).toInt(), 0, 0)
        binding.randomBigImg.shapeAppearanceModel = binding.randomBigImg
                .shapeAppearanceModel
                .toBuilder()
                .setAllCornerSizes(resources.getDimension(R.dimen.charts_corner_radius))
                .build()

        viewModel.track.observe(viewLifecycleOwner) {
            it ?: return@observe
            if (it.type == Stuff.TYPE_TRACKS) {
                viewModel.totalScrobbles = it.total
            } else if (it.type == Stuff.TYPE_LOVES) {
                viewModel.totalLoves = it.total
                it.track?.isLoved = true
            }
            if (it.track != null)
                prefs.edit()
                        .putInt(Stuff.PREF_ACTIVITY_LAST_RANDOM_TYPE, it.type)
                        .apply()
            setTrack(it.track)
        }
        if (viewModel.track.value == null) {
            val type = prefs.getInt(Stuff.PREF_ACTIVITY_LAST_RANDOM_TYPE, Stuff.TYPE_TRACKS)
            if (type == Stuff.TYPE_TRACKS)
                viewModel.loadRandomScrobble()
            else
                viewModel.loadRandomLove()
            setLoading(true)
        }
        binding.randomTrackContainer.post { setContainerWidth() }
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
        if (loading) {
            if (!Main.isOnline) {
                binding.randomStatus.text = getString(R.string.unavailable_offline)
                binding.randomStatus.visibility = View.VISIBLE
                binding.randomProgress.hide()
                isLoading = false
            } else {
                binding.randomStatus.visibility = View.GONE
                binding.randomProgress.show()
            }
            TransitionManager.beginDelayedTransition(binding.root,
                    Fade().setInterpolator(DecelerateInterpolator()))
            binding.randomTrackContainer.visibility = View.INVISIBLE
            binding.randomTrackButtons.visibility = View.INVISIBLE
        } else {
            binding.randomProgress.hide()
        }
    }

    private fun setTrack(track: Track?) {
        setLoading(false)
        if (track == null) {
            binding.randomStatus.text = getString(R.string.random_not_found)
            binding.randomStatus.visibility = View.VISIBLE
            return
        }
        binding.randomStatus.visibility = View.GONE
        TransitionManager.beginDelayedTransition(binding.root,
                Fade().setInterpolator(DecelerateInterpolator()))
        binding.randomTrackContainer.visibility = View.VISIBLE
        binding.randomTrackButtons.visibility = View.VISIBLE

        var name = track.name
        if (track.isLoved)
            name = "❤ " + name

        binding.randomTrack.recentsTitle.text = name
        binding.randomTrack.recentsSubtitle.text = track.artist
        binding.randomTrack.recentsDate.text = Stuff.myRelativeTime(context!!, track.playedWhen?.time ?: 0, true)

        binding.randomPlay.setOnClickListener {
            Stuff.launchSearchIntent(track.artist, track.name, context!!)
        }
        binding.randomTrack.root.setOnClickListener {
            Stuff.launchSearchIntent(track.artist, track.name, context!!)
        }
        binding.randomInfo.setOnClickListener {
            if (track.url != null) {
                val info = InfoFragment()
                val b = Bundle()
                b.putString(NLService.B_ARTIST, track.artist)
                if (!track.album.isNullOrEmpty())
                    b.putString(NLService.B_ALBUM, track.album)
                b.putString(NLService.B_TITLE, track.name)
                info.arguments = b
                info.show(parentFragmentManager, null)
            }
        }
        var imgUrl = track.getWebpImageURL(ImageSize.EXTRALARGE)?.replace("300x300", "600x600")
        if (imgUrl?.isEmpty() == true)
            imgUrl = null
        Picasso.get()
                .load(imgUrl)
                .placeholder(R.drawable.vd_wave_simple)
                .error(R.drawable.vd_wave_simple)
                .into(binding.randomBigImg)

    }

    private fun setContainerWidth() {
        val lp = binding.randomTrackContainer.layoutParams as LinearLayout.LayoutParams
        val maxWidth = resources.getDimension(R.dimen.random_container_max_width)
        if (resources.displayMetrics.widthPixels > maxWidth)
            lp.width = maxWidth.toInt()
        else
            lp.width = LinearLayout.LayoutParams.MATCH_PARENT

        binding.randomTrackContainer.layoutParams = lp
    }
}