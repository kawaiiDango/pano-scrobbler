package com.arn.scrobble.onboarding

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.MainNotifierViewModel
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.databinding.ContentOnboardingBinding
import com.arn.scrobble.scrobbleable.Scrobblables
import ernestoyaquello.com.verticalstepperform.Step
import ernestoyaquello.com.verticalstepperform.listener.StepperFormListener

class OnboardingFragment : Fragment(), StepperFormListener {
    private var _binding: ContentOnboardingBinding? = null
    val binding
        get() = _binding!!
    private lateinit var notificationPermRequest: ActivityResultLauncher<String>
    private lateinit var onboardingSteps: OnboardingSteps
    private val mainNotifierViewModel by activityViewModels<MainNotifierViewModel>()

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
        _binding = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
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

        if (Stuff.isDkmaNeeded())
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
        if (mainNotifierViewModel.userStackDepth == 0)
            mainNotifierViewModel.pushUser(Scrobblables.current!!.userAccount.user)
        findNavController().apply {
            if (graph.startDestinationId != R.id.myHomePagerFragment)
                setGraph(R.navigation.nav_graph) // reset
            else
                navigate(R.id.myHomePagerFragment)
        }
    }

    override fun onCancelledForm() {
    }

    override fun onStepAdded(index: Int, addedStep: Step<*>?) {
    }

    override fun onStepRemoved(index: Int) {
    }

}