package com.arn.scrobble

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
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
import kotlinx.android.synthetic.main.dialog_fix_it.view.*


class FixItFragment: BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_fix_it, container, false)

        view.fix_it_startup_action.setOnClickListener {
            val startupMgrIntent = Stuff.getStartupIntent(context!!)
            FirstThingsFragment.openStartupMgr(startupMgrIntent, context!!)
        }
        addTouchDelegate(view.fix_it_startup_action, 24, 10)
        if (!Main.isTV) {
            view.fix_it_nls.visibility = View.VISIBLE
            view.fix_it_nls_action.setOnClickListener {
                startActivity(Intent(Stuff.NLS_SETTINGS))
            }
            addTouchDelegate(view.fix_it_nls_action, 24, 10)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val batteryIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            if (activity!!.packageManager.queryIntentActivities(batteryIntent,
                            PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()) {
                view.fix_it_battery.visibility = View.VISIBLE
                view.fix_it_battery_action.setOnClickListener {
                    startActivity(batteryIntent)
                }
            }
            addTouchDelegate(view.fix_it_battery_action, 24, 10)
        }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val width = resources.getDimension(R.dimen.bottom_sheet_width)
            if (width > 0)
                dialog.window!!.setLayout(width.toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()

        val bottomSheetView = dialog!!.window!!.decorView.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        BottomSheetBehavior.from(bottomSheetView).isHideable = false
        if (view?.isInTouchMode == false)
            BottomSheetBehavior.from(bottomSheetView).state = BottomSheetBehavior.STATE_EXPANDED
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