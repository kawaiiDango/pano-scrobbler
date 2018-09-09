package com.arn.scrobble

import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.dialog_fix_it.*


class FixItFragment: BottomSheetDialogFragment() {
    private var startupMgrIntent: Intent? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_fix_it, container, false)
    }

    override fun onStart() {
        super.onStart()
        startupMgrIntent = Stuff.getStartupIntent(context!!)
        if (startupMgrIntent != null) {
            fix_it_startup_desc.text = getString(R.string.fix_it_startup_desc_sure)
            fix_it_startup_action.visibility = View.VISIBLE
            fix_it_startup_action.setOnClickListener {
                startActivity(startupMgrIntent)
            }
            addTouchDelegate(fix_it_startup_action, 24, 10)
        }

        fix_it_nls_action.setOnClickListener {
            startActivity(Intent(Stuff.NLS_SETTINGS))
        }
        addTouchDelegate(fix_it_nls_action, 24, 10)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fix_it_battery.visibility = View.VISIBLE
            fix_it_battery_action.setOnClickListener {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
        addTouchDelegate(fix_it_battery_action, 24, 10)

        val bottomSheetView = dialog.window!!.decorView.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        BottomSheetBehavior.from(bottomSheetView).isHideable = false
    }

    private fun addTouchDelegate(child:View, vertAreaDp: Int, horzAreaDp: Int) {
        val vertArea = Stuff.dp2px(vertAreaDp, context!!)
        val horzArea = Stuff.dp2px(horzAreaDp, context!!)
        val parent = child.parent as View

        parent.post {
            val delegateArea = Rect()

            child.getHitRect(delegateArea)
            delegateArea.top -= vertArea
            delegateArea.bottom += vertArea
            delegateArea.left -= horzArea
            delegateArea.right += horzArea

            val touchDelegate = TouchDelegate(delegateArea, child)
            parent.touchDelegate = touchDelegate
        }

    }

}