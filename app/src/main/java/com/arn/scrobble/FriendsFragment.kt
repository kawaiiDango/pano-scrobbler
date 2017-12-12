package com.arn.scrobble

import android.app.AlertDialog
import android.app.Fragment
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import com.arn.scrobble.ui.EndlessScrollListener
import de.umass.lastfm.User
import kotlinx.android.synthetic.main.content_friends.*
import kotlinx.android.synthetic.main.grid_item_friend.view.*
import kotlin.math.max
import kotlin.math.min

/**
 * Created by arn on 09/07/2017.
 */

class FriendsFragment : Fragment() {

    private var adapter: FriendsAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.content_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FriendsAdapter(activity, R.layout.grid_item_friend)
        friends_grid.adapter = adapter
        friends_grid.setOnScrollListener(loadMoreListener)
        friends_grid.onItemClickListener = profileClickListener
        Stuff.setProgressCircleColor(friends_swipe_refresh)
        friends_swipe_refresh.setOnRefreshListener { adapter?.loadFriends(1) }
    }

    override fun onPause() {
        super.onPause()
        (friends_grid.adapter as FriendsAdapter?)
                ?.handler?.removeMessages(Stuff.CANCELLABLE_MSG)
    }

    override fun onResume() {
        super.onResume()
        Stuff.setTitle(activity, R.string.friends)
        adapter?.loadFriends(1)
    }

    private val profileClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
        val gridItem = adapter?.getView(position, null, null) // force inflate
        if (gridItem != null){
            gridItem.friends_links.visibility = View.VISIBLE
            gridItem.friends_pic.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            gridItem.friends_pic.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            val userLink = (friends_grid.getItemAtPosition(position) as User?)?.url ?: return@OnItemClickListener
            gridItem.friends_profile.setOnClickListener { v:View ->
                Stuff.openInBrowser(userLink, activity, v)
            }
            gridItem.friends_profile.compoundDrawables
            gridItem.friends_loved.setOnClickListener { v:View ->
                Stuff.openInBrowser(userLink + "/loved", activity, v)
            }
            gridItem.friends_chart.setOnClickListener { v:View ->
                Stuff.openInBrowser(userLink + "/listening-report/week", activity, v)
            }
            drawableTintCompat(gridItem.friends_profile)
            drawableTintCompat(gridItem.friends_loved)
            drawableTintCompat(gridItem.friends_chart)
            gridItem.background = view.background
        } else
            return@OnItemClickListener


        val dialog = AlertDialog.Builder(activity)
                .setView(gridItem)
                .create()

        dialog.window.attributes.windowAnimations = R.style.TransFadeScale
        dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(true)
        /*
        view.requestLayout()
        parent.requestLayout()

        val vv= dialog.window.reenterTransition

        dialog.window.attributes.gravity = Gravity.TOP or Gravity.LEFT
        dialog.window.attributes.x = 440
        dialog.window.attributes.y = 750
        val anim = dialog.window.attributes.layoutAnimationParameters
        Stuff.log("anim: $anim")
*/
        val centerPos = intArrayOf(0,0)
        view.getLocationOnScreen(centerPos)
        centerPos[0] += view.width/2
        centerPos[1] += view.height/2

        dialog.setOnShowListener {
            revealShow(gridItem, centerPos[0], centerPos[1], true, dialog)
        }
//        dialog.setOnCancelListener { revealShow(gridItem, centerPos[0], centerPos[1], false, dialog) }

        dialog.show()

    }

    private fun revealShow(view: View, xAbs:Int, yAbs:Int, reveal: Boolean, dialog: AlertDialog) {
        val w = view.width
        val h = view.height
        val dPos = intArrayOf(0,0)

        view.getLocationOnScreen(dPos)

        val x = max(dPos[0], min(dPos[0]+w, xAbs)) - dPos[0]
        val y = max(dPos[1], min(dPos[1]+h, yAbs)) - dPos[1]
        val maxRadius = Math.sqrt((w * w / 4 + h * h / 4).toDouble()).toFloat()

        if (reveal) {
            val revealAnimator = ViewAnimationUtils.createCircularReveal(view,
                    x, y, 0f, maxRadius)

            view.visibility = View.VISIBLE
            revealAnimator.start()

        } else {

            val anim = ViewAnimationUtils.createCircularReveal(view, x, y, maxRadius, 0f)
/*
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    dialog.dismiss()
                    view.visibility = View.INVISIBLE

                }
            })

            anim.start()
            */
        }

    }
    @Suppress("DEPRECATION")
    private fun drawableTintCompat(view: TextView, color:Int = resources.getColor(R.color.colorAccent)){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            view.compoundDrawables?.forEach {
                it?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            }
        }
    }

    private val loadMoreListener = object : EndlessScrollListener() {
        override fun onLoadMore(page: Int, totalItemsCount: Int): Boolean {
            return adapter?.loadFriends(page) ?: false
            // true ONLY if more data is actually being loaded; false otherwise.
        }
    }
}