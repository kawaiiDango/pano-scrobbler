package com.arn.scrobble

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.arn.scrobble.info.InfoFragment
import com.squareup.picasso.Picasso
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.content_random.*
import kotlinx.android.synthetic.main.list_item_recents.view.*


/**
 * Created by arn on 06/09/2017.
 */
class RandomFragment: Fragment() {

    private val viewModel by lazy { VMFactory.getVM(this, RandomVM::class.java) }
    private val prefs by lazy { context!!.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE) }
    private var isLoading = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.content_random, container, false)
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
        random_get_loved.setOnClickListener {
            if (!isLoading) {
                viewModel.loadRandomLove()
                setLoading(true)
            }
        }
        random_get_scrobble.setOnClickListener {
            if (!isLoading) {
                viewModel.loadRandomScrobble()
                setLoading(true)
            }
        }
        random_track.recents_menu.visibility = View.INVISIBLE
        random_track.recents_playing.visibility = View.INVISIBLE
        random_track.recents_img.visibility = View.GONE
        random_track.recents_img_overlay.visibility = View.GONE
        random_track.recents_date.setTextColor(Color.WHITE)
        random_track.setBackgroundResource(R.drawable.layer_random_track)
        random_track.setPaddingRelative(0, resources.getDimension(R.dimen.gradient_container_top_padding).toInt(), 0, 0)

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
        random_track_container.post { setContainerWidth() }
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
        if (loading) {
            if (!Main.isOnline) {
                random_status.text = getString(R.string.unavailable_offline)
                random_status.visibility = View.VISIBLE
                random_progress.visibility = View.GONE
                isLoading = false
            } else {
                random_status.visibility = View.GONE
                random_progress.visibility = View.VISIBLE
            }
            random_track_container.visibility = View.INVISIBLE
            random_track_buttons.visibility = View.INVISIBLE
        } else {
            random_progress.visibility = View.GONE
        }
    }

    private fun setTrack(track: Track?) {
        setLoading(false)
        if (track == null) {
            random_status.text = getString(R.string.random_not_found)
            random_status.visibility = View.VISIBLE
            return
        }
        random_status.visibility = View.GONE
        random_track_container.visibility = View.VISIBLE
        random_track_buttons.visibility = View.VISIBLE

        var name = track.name
        if (track.isLoved)
            name = "❤ " + name

        random_track.recents_title.text = name
        random_track.recents_subtitle.text = track.artist
        random_track.recents_date.text = Stuff.myRelativeTime(context!!, track.playedWhen?.time ?: 0, true)

        random_play.setOnClickListener {
            Stuff.launchSearchIntent(track.artist, track.name, context!!)
        }
        random_track.setOnClickListener {
            Stuff.launchSearchIntent(track.artist, track.name, context!!)
        }
        random_info.setOnClickListener {
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
                .into(random_big_img)
    }

    private fun setContainerWidth() {
        val lp = random_track_container.layoutParams as LinearLayout.LayoutParams
        val maxWidth = resources.getDimension(R.dimen.random_container_max_width)
        if (resources.displayMetrics.widthPixels > maxWidth)
            lp.width = maxWidth.toInt()
        else
            lp.width = LinearLayout.LayoutParams.MATCH_PARENT

        random_track_container.layoutParams = lp
    }
}