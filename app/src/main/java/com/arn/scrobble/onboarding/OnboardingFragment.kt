package com.arn.scrobble.onboarding

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.GravityCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.arn.scrobble.MainActivity
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ContentOnboardingBinding
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.UiUtils.setTitle
import ernestoyaquello.com.verticalstepperform.Step
import ernestoyaquello.com.verticalstepperform.listener.StepperFormListener

class OnboardingFragment : Fragment(), StepperFormListener {
    private var _binding: ContentOnboardingBinding? = null
    val binding
        get() = _binding!!
    private lateinit var notificationPermRequest: ActivityResultLauncher<String>
    private lateinit var onboardingSteps: OnboardingSteps

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationPermRequest =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->

            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ContentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        (activity as AppCompatActivity?)!!.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        _binding = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        setTitle(0)
        (activity as AppCompatActivity?)!!.supportActionBar?.setDisplayHomeAsUpEnabled(false)

        // a library bug?
        (activity as MainActivity).binding.coordinatorMain.appBar.setExpanded(
            expanded = false,
            animate = false
        )

        fixFocusBug()
    }

    override fun onResume() {
        super.onResume()
        (binding.stepperForm.openStep as? OnboardingSteps.OnboardingStep)
            ?.continueIfDataValid()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onboardingSteps = OnboardingSteps(this)

        onboardingSteps.addStep { LoginStep() }
        onboardingSteps.addStep { NotificationListenerStep() }

        if (Stuff.isDkmaNeeded(context!!))
            onboardingSteps.addStep { DkmaStep() }

        onboardingSteps.addStep { ChooseAppsStep() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onboardingSteps.addStep { SendNotificationsStep(notificationPermRequest) }
        }

        binding.stepperForm
            .setup(this, onboardingSteps.stepsList)
            .init()

    }

    private fun fixFocusBug() {
        binding.stepperForm.findViewById<LinearLayout>(ernestoyaquello.com.verticalstepperform.R.id.content)
            .children.forEach { form ->
                form.isClickable = false
                form.isFocusable = false
                form.findViewById<LinearLayout>(ernestoyaquello.com.verticalstepperform.R.id.step_header)
                    .apply {
                        isClickable = false
                        isFocusable = false
                    }

            }
    }

    override fun onCompletedForm() {
        val activity = activity!! as MainActivity
        activity.showHomePager()
        if (activity.coordinatorPadding == 0)
            activity.binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    override fun onCancelledForm() {
    }

    override fun onStepAdded(index: Int, addedStep: Step<*>?) {
    }

    override fun onStepRemoved(index: Int) {
    }

    companion object {
        fun isNotificationListenerEnabled(c: Context): Boolean {
            val packages = NotificationManagerCompat.getEnabledListenerPackages(c)
            return packages.any { it == c.packageName }
        }

        fun isLoggedIn(prefs: MainPrefs): Boolean {
            return !(prefs.lastfmSessKey == null || prefs.lastfmUsername == null)
        }
    }
}