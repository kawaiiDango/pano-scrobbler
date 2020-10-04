package com.arn.scrobble

import android.content.res.Configuration
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.arn.scrobble.ui.FocusChangeListener
import com.arn.scrobble.ui.ItemClickListener
import kotlinx.android.synthetic.main.content_similar.*
import kotlinx.android.synthetic.main.coordinator_main.*
import kotlinx.android.synthetic.main.coordinator_main.view.*
import kotlinx.android.synthetic.main.header_default.*
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Created by arn on 29/12/2017.
 */
class SimilarTracksFragment : Fragment(), ItemClickListener, FocusChangeListener {
    private val viewModel by lazy { VMFactory.getVM(this, TracksVM::class.java) }
    private lateinit var adapter: SimilarTracksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Slide(Gravity.TOP)
        returnTransition = Slide(Gravity.TOP)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.content_similar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.isNestedScrollingEnabled = false
        adapter = SimilarTracksAdapter(view, viewModel)

        val glm = GridLayoutManager(context!!, 1)
        calcGridSize(glm)

        similar_grid.layoutManager = glm
        adapter.clickListener = this
        adapter.focusChangeListener = this
        similar_grid.adapter = adapter
        header_text.text = getString(R.string.similar_tracks)

        val activity = activity!!
        if (activity.ctl.hero_calendar.tag == true)
            activity.ctl.hero_calendar.visibility = View.GONE
        activity.ctl.hero_similar.visibility = View.GONE
        activity.ctl.sparkline_frame.visibility = View.GONE
        activity.ctl.hero_title.visibility = View.VISIBLE

        viewModel.similarReceiver.observe(viewLifecycleOwner, {
            it ?: return@observe
            adapter.populate(it)

            it.forEachIndexed { i, t ->
                viewModel.loadInfo(t, i)
            }
            viewModel.similarReceiver.value = null
        })

        viewModel.trackInfo.observe(viewLifecycleOwner, {
            it ?: return@observe
            adapter.setImg(it.first,  it.second?.imageUrlsMap)
        })
    }

    override fun onDestroyView() {
        val activity = activity!!
        activity.ctl.hero_similar.isEnabled = true
        if (activity.ctl.hero_similar.isInTouchMode)
            activity.ctl.hero_similar.visibility = View.VISIBLE
        else
            activity.ctl.hero_similar.visibility = View.INVISIBLE
        activity.ctl.hero_title.visibility = View.GONE
        if (activity.ctl.hero_calendar.tag == true)
            activity.ctl.hero_calendar.visibility = View.VISIBLE
        activity.ctl.sparkline_frame.visibility = View.VISIBLE
        Stuff.setAppBarHeight(activity)
        super.onDestroyView()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (isVisible)
            Stuff.setAppBarHeight(activity!!, adapter.itemSizeDp*3)
        val glm = similar_grid?.layoutManager as GridLayoutManager? ?: return
        calcGridSize(glm)
    }

    private fun calcGridSize(glm: GridLayoutManager) {
        val cols: Int
        var w = resources.getDimension(R.dimen.grid_size).roundToInt()
        val screenW = resources.displayMetrics.widthPixels
        cols = screenW / w
        w += (screenW % w)/cols
        var rows = max(resources.displayMetrics.heightPixels/3/w, 3)
        if (rows * cols <= 6)
            rows++

        adapter.itemSizeDp = w/resources.displayMetrics.densityDpi
        glm.spanCount = cols
        val arguments = arguments!!
        if (viewModel.tracks.size != rows * cols)
            viewModel.loadSimilar(arguments.getString("artist")!!, arguments.getString("track")!!, rows * cols)
        else
            adapter.populate(viewModel.tracks)
    }

    override fun onItemClick (view: View, position: Int) {
        val adapter = similar_grid.adapter as SimilarTracksAdapter
        val track = adapter.getItem(position)
        Stuff.launchSearchIntent(track.artist, track.name, context!!)
    }

    //only called when !view.isInTouchMode
    override fun onFocus(view: View, position: Int) {
        val pos = IntArray(2)
        view.getLocationInWindow(pos)

        if (pos[1] + view.height > activity!!.coordinator.height && activity!!.app_bar.isExpanded)
            activity!!.app_bar.setExpanded(false, true)
    }
}