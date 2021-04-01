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
import com.arn.scrobble.Stuff.dp
import com.arn.scrobble.databinding.DialogFixItBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class FixItFragment: BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = DialogFixItBinding.inflate(inflater, container, false)

        binding.fixItStartupAction.setOnClickListener {
            val startupMgrIntent = Stuff.getStartupIntent(context!!)
            FirstThingsFragment.openStartupMgr(startupMgrIntent, context!!)
        }
        addTouchDelegate(binding.fixItStartupAction, 24.dp, 10.dp)
        if (!Main.isTV) {
            binding.fixItNls.visibility = View.VISIBLE
            binding.fixItNlsAction.setOnClickListener {
                startActivity(Intent(Stuff.NLS_SETTINGS))
            }
            addTouchDelegate(binding.fixItNlsAction, 24.dp, 10.dp)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val batteryIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            if (activity!!.packageManager.queryIntentActivities(batteryIntent,
                            PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()) {
                binding.fixItBattery.visibility = View.VISIBLE
                binding.fixItBatteryAction.setOnClickListener {
                    startActivity(batteryIntent)
                }
            }
            addTouchDelegate(binding.fixItBatteryAction, 24.dp, 10.dp)
        }
        return binding.root
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

    private fun addTouchDelegate(child:View, vertArea: Int, horzArea: Int) {
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