package com.arn.scrobble.charts

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.text.format.DateFormat
import android.view.*
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.arn.scrobble.*
import com.arn.scrobble.info.InfoFragment
import com.arn.scrobble.ui.EndlessRecyclerViewScrollListener
import com.arn.scrobble.ui.ItemClickListener
import com.arn.scrobble.ui.ListViewItemHighlightTvHack
import com.arn.scrobble.ui.SimpleHeaderDecoration
import com.google.android.material.chip.Chip
import de.umass.lastfm.*
import kotlinx.android.synthetic.main.content_charts.*
import java.util.*
import kotlin.math.roundToInt


open class ChartBaseFragment: Fragment(), ItemClickListener {

    private lateinit var adapter: ChartsAdapter
    open val type = 0
    private val viewmodel by lazy { VMFactory.getVM(this, ChartsVM::class.java) }
    private val periodChipIds = arrayOf(R.id.charts_choose_week, R.id.charts_7day, R.id.charts_1month, R.id.charts_3month, R.id.charts_6month, R.id.charts_12month, R.id.charts_overall)
    private val username: String?
        get() = parentFragment?.arguments?.getString(Stuff.ARG_USERNAME)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.content_charts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        charts_period.post { setChipGroupGravity() }
    }

    override fun onResume() {
        super.onResume()
        if (charts_grid?.adapter == null)
            postInit()
    }

    override fun onPause() {
        if (charts_grid?.adapter != null) {
            adapter.removeHandlerCallbacks()
            viewmodel.removeAllInfoTasks()
        }
        super.onPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val glm = charts_grid?.layoutManager as GridLayoutManager?
        glm?.spanCount = getNumColumns()
        setChipGroupGravity()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.charts_menu, menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_share)
            share()
        return super.onOptionsItemSelected(item)
    }

    private fun postInit() {
        val pref = context?.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE)
        val periodIdx = pref?.getInt(Stuff.PREF_ACTIVITY_LAST_CHARTS_PERIOD, 1) ?: 1
        var firstLoad = charts_period.checkedChipId == View.NO_ID
        adapter = ChartsAdapter(view!!)
        viewmodel.username = username
        viewmodel.periodIdx = periodIdx
        charts_period.setOnCheckedChangeListener { group, checkedId ->
            adapter.removeHandlerCallbacks()
            viewmodel.removeAllInfoTasks()
            viewmodel.reachedEnd = false
            val idx = periodChipIds.indexOf(checkedId)
            if (checkedId != R.id.charts_choose_week) {
                if (viewmodel.periodIdx != idx || firstLoad) {
                    viewmodel.periodIdx = idx
                    loadCharts(1)
                    pref?.edit()?.putInt(Stuff.PREF_ACTIVITY_LAST_CHARTS_PERIOD, idx)?.apply()
                }
                setWeeklyChipName(false)
            } else if (firstLoad){
                val from = pref?.getLong(Stuff.PREF_ACTIVITY_LAST_CHARTS_WEEK_FROM, 0) ?: 0
                val to = pref?.getLong(Stuff.PREF_ACTIVITY_LAST_CHARTS_WEEK_TO, 0) ?: 0
                viewmodel.weeklyChart = Chart(Date(from), Date(to), emptyList())
                viewmodel.loadWeeklyCharts(type)
                setWeeklyChipName(true)
            }
            firstLoad = false

            val hsv = group.parent as HorizontalScrollView
            val chip = group.findViewById<Chip>(checkedId)
            group.post {
                val scrollBounds = Rect()
                hsv.getDrawingRect(scrollBounds)
                val left = chip.x
                val right = left + chip.width
                val isChipVisible = scrollBounds.left < left && scrollBounds.right > right
                if (!isChipVisible)
                    hsv.smoothScrollTo(chip.left, chip.top)
            }
        }
        charts_choose_week.setOnClickListener {
            if (Main.isOnline)
                charts_progress.visibility = View.VISIBLE
            viewmodel.loadWeeklyChartsList(parentFragment!!.arguments?.getLong(Stuff.ARG_REGISTERED_TIME, 0) ?: 0)
        }
        charts_period.check(periodChipIds[periodIdx])

        val glm = GridLayoutManager(context!!, getNumColumns())
        charts_grid.layoutManager = glm
        (charts_grid.itemAnimator as SimpleItemAnimator?)?.supportsChangeAnimations = false
        charts_grid.adapter = adapter
        charts_grid.addItemDecoration(SimpleHeaderDecoration(0, Stuff.dp2px(25, context!!)))

        val loadMoreListener = object : EndlessRecyclerViewScrollListener(glm) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                loadCharts(page)
            }
        }
        loadMoreListener.currentPage = viewmodel.page
        charts_grid.addOnScrollListener(loadMoreListener)
        adapter.loadMoreListener = loadMoreListener
        adapter.clickListener = this
        adapter.viewModel = viewmodel
        viewmodel.chartsReceiver.observe(viewLifecycleOwner, {
            if (it == null && !Main.isOnline && viewmodel.chartsData.size == 0)
                adapter.populate()
            it ?: return@observe
            if (it.second.isEmpty())
                viewmodel.reachedEnd = true
            val page = it.first
            synchronized(viewmodel.chartsData) {
                if (page == 1)
                    viewmodel.chartsData.clear()
                viewmodel.chartsData.addAll(it.second)
            }
            loadMoreListener.currentPage = page
            adapter.populate()
            viewmodel.chartsReceiver.value = null
        })
        viewmodel.weeklyListReceiver.observe(viewLifecycleOwner, { weeklyList ->
            weeklyList ?: return@observe
            val weeklyStrList = arrayListOf<String>()
            weeklyList.forEach {
                weeklyStrList += getString(
                        R.string.a_to_b,
                        DateFormat.getMediumDateFormat(context).format(it.from.time),
                        DateFormat.getMediumDateFormat(context).format(it.to.time)
                )
            }
            val dialog = AlertDialog.Builder(context!!, R.style.DarkDialog)
                    .setTitle(Stuff.getColoredTitle(context!!, getString(R.string.charts_choose_week)))
                    .setItems(weeklyStrList.toTypedArray()) { dialogInterface, i ->
                        val item = weeklyList[i]
                        viewmodel.weeklyChart = item
                        viewmodel.periodIdx = 0
                        viewmodel.loadWeeklyCharts(type)
                        pref?.edit()
                                ?.putLong(Stuff.PREF_ACTIVITY_LAST_CHARTS_WEEK_FROM, item.from.time)
                                ?.putLong(Stuff.PREF_ACTIVITY_LAST_CHARTS_WEEK_TO, item.to.time)
                                ?.putInt(Stuff.PREF_ACTIVITY_LAST_CHARTS_PERIOD, 0)
                                ?.apply()
                        setWeeklyChipName(true)
                    }
                    .setOnCancelListener {
                        charts_period?.check(periodChipIds[viewmodel.periodIdx])
                    }
                    .setNegativeButton(android.R.string.cancel) { dialogInterface, i ->
                        charts_period?.check(periodChipIds[viewmodel.periodIdx])
                    }
                    .show()
            var pos = 0
            for (i in weeklyList.indices) {
                val chart = weeklyList[i]
                if (viewmodel.weeklyChart == null)
                    break
                if (chart.from.time == viewmodel.weeklyChart!!.from.time && chart.to.time == viewmodel.weeklyChart!!.to.time) {
                    pos = i
                    break
                }
            }
            if (pos < weeklyList.size)
                dialog.listView.setSelection(pos)
            dialog.listView.onItemSelectedListener = ListViewItemHighlightTvHack()

            charts_progress.visibility = View.GONE
        })
        viewmodel.info.observe(viewLifecycleOwner, {
            it ?: return@observe
            val imgUrl = when (val entry = it.second) {
                is Artist -> entry.getImageURL(ImageSize.EXTRALARGE) ?: ""
                is Album -> entry.getWebpImageURL(ImageSize.EXTRALARGE) ?: ""
                is Track -> entry.getWebpImageURL(ImageSize.EXTRALARGE) ?: ""
                else -> ""
            }
            adapter.setImg(it.first, imgUrl)
            viewmodel.removeInfoTask(it.first)
        })

        if (viewmodel.chartsData.isNotEmpty())
            adapter.populate()
    }

    private fun loadCharts(page: Int) {
        charts_grid ?: return
        if (viewmodel.reachedEnd) {
            adapter.loadMoreListener.isAllPagesLoaded = true
            return
        }
        viewmodel.loadCharts(type, page)
    }

    private fun setWeeklyChipName(set: Boolean) {
        if (set)
            charts_choose_week.text = getString(
                R.string.a_to_b,
                DateFormat.getMediumDateFormat(context).format(viewmodel.weeklyChart!!.from.time),
                DateFormat.getMediumDateFormat(context).format(viewmodel.weeklyChart!!.to.time)
        )
        else
            charts_choose_week.text = getString(R.string.charts_choose_week)
        charts_choose_week.invalidate()
    }

    private fun setChipGroupGravity() {
        val lp = charts_period.layoutParams as FrameLayout.LayoutParams
        if (resources.displayMetrics.widthPixels > charts_period.width && lp.gravity != Gravity.CENTER_HORIZONTAL) {
            lp.gravity = Gravity.CENTER_HORIZONTAL
            charts_period.layoutParams = lp
        } else if (resources.displayMetrics.widthPixels <= charts_period.width && lp.gravity != Gravity.START) {
            lp.gravity = Gravity.START
            charts_period.layoutParams = lp
        }
    }

    private fun share() {
        val entries = viewmodel.chartsData
        if (entries.isNullOrEmpty())
            return
        val pref = context?.getSharedPreferences(Stuff.ACTIVITY_PREFS, Context.MODE_PRIVATE) ?: return
        val type = when (entries[0]) {
            is Artist -> getString(R.string.artists)
            is Album -> getString(R.string.albums)
            else -> getString(R.string.tracks)
        }
        val checkedChip = charts_period.findViewById<Chip>(charts_period.checkedChipId)
        val period = if (checkedChip.id == R.id.charts_choose_week) {
            viewmodel.weeklyChart ?: return
            getString(
                    R.string.weekly_range,
                    DateFormat.getMediumDateFormat(context).format(viewmodel.weeklyChart!!.from.time),
                    DateFormat.getMediumDateFormat(context).format(viewmodel.weeklyChart!!.to.time)
            )
        } else
            checkedChip.text.toString()
        var pos = 1
        val list = entries.take(10).joinToString(separator = "\n") {
            when (it) {
                is Track -> getString(R.string.charts_num_text, pos++, it.artist + " — " + it.name)
                is Album -> getString(R.string.charts_num_text, pos++, it.artist + " — " + it.name)
                else -> getString(R.string.charts_num_text, pos++, it.name)
            }
        }
        val user = if (username != null)
            getString(R.string.possesion, username)
        else
            getString(R.string.my)
        var shareText = getString(R.string.charts_share_text,
                user, period.toLowerCase(), type.toLowerCase(), list)

        shareText += "\n\n" + pref.getString(Stuff.PREF_ACTIVITY_SHARE_SIG,
                getString(R.string.share_sig, getString(R.string.share_link)))
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_SUBJECT, shareText)
        i.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(i, getString(R.string.share_this_chart)))
    }

    override fun onItemClick(view: View, position: Int) {
        when (val item = adapter.getItem(position)) {
            is Artist -> {
                val info = InfoFragment()
                val b = Bundle()
                b.putString(NLService.B_ARTIST, item.name)
                b.putString(Stuff.ARG_USERNAME, username)
                info.arguments = b
                info.show(activity!!.supportFragmentManager, null)
            }
            is Album -> {
                val info = InfoFragment()
                val b = Bundle()
                b.putString(NLService.B_ARTIST, item.artist)
                b.putString(NLService.B_ALBUM, item.name)
                b.putString(Stuff.ARG_USERNAME, username)
                info.arguments = b
                info.show(activity!!.supportFragmentManager, null)
            }
            is Track -> {
                val info = InfoFragment()
                val b = Bundle()
                b.putString(NLService.B_ARTIST, item.artist)
                b.putString(NLService.B_ALBUM, item.album)
                b.putString(NLService.B_TITLE, item.name)
                b.putString(Stuff.ARG_USERNAME, username)
                info.arguments = b
                info.show(activity!!.supportFragmentManager, null)
            }
        }
    }

    private fun getNumColumns(): Int {
        return resources.displayMetrics.widthPixels /
                resources.getDimension(R.dimen.big_grid_size).roundToInt()
    }
}