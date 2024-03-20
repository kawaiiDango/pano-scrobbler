package com.arn.scrobble.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.arn.scrobble.R
import com.arn.scrobble.databinding.ListItemVerticalStepperBinding
import com.arn.scrobble.ui.GenericDiffCallback

class VerticalStepperAdapter(
    private val viewModel: OnboardingVM,
    private val loginStepView: () -> View,
    private val onCompleted: () -> Unit
) :
    ListAdapter<OnboardingStepData, VerticalStepperAdapter.OnboardingStepVH>(
        GenericDiffCallback { old, new -> old.type == new.type }) {

    private val loginStepPosition = 0

    var selectedPos: Int = viewModel.selectedPosition
        set(value) {
            val oldPos = field
            field = value
            viewModel.selectedPosition = value
            notifyItemChanged(oldPos)
            notifyItemChanged(value)
        }

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingStepVH {
        val binding = ListItemVerticalStepperBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingStepVH(binding)
    }

    override fun onBindViewHolder(holder: OnboardingStepVH, position: Int) {
        holder.setData(getItem(position))
    }

    fun skipToNextStep() {
        viewModel.skippedPositions += selectedPos
        checkIfStepsCompleted()
    }

    fun checkIfStepsCompleted() {
        var pos = selectedPos
        while (pos < itemCount) {
            if (getItem(pos).isCompleted() || pos in viewModel.skippedPositions) {
                pos += 1
            } else {
                break
            }
        }
        if (pos == itemCount) {
            onCompleted()
        } else {
            selectedPos = pos
        }
    }

    inner class OnboardingStepVH(private val binding: ListItemVerticalStepperBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(stepData: OnboardingStepData) {
            val isSelected = selectedPos == bindingAdapterPosition
            val isCompleted = stepData.isCompleted()
            itemView.alpha = if (isSelected) 1f else 0.5f

            binding.stepTitle.text = stepData.title
            binding.stepDescription.isVisible = stepData.description != null && isSelected
            binding.stepDescription.text = stepData.description
            binding.stepCustomLayout.apply {
                if (bindingAdapterPosition == loginStepPosition && isSelected) {
                    addView(loginStepView())
                } else {
                    removeAllViews()
                }
            }

            binding.stepButtons.root.isVisible =
                isSelected && bindingAdapterPosition != loginStepPosition
            binding.stepButtons.openButton.setOnClickListener {
                stepData.openAction()
            }
            binding.stepButtons.skipButton.isVisible = stepData.canSkip
            binding.stepButtons.skipButton.setOnClickListener {
                if (stepData.skipAction != null)
                    stepData.skipAction.invoke()
                else
                    skipToNextStep()
            }

            binding.stepBullet.setImageResource(
                if (isSelected)
                    R.drawable.vd_circle
                else
                    R.drawable.vd_arrow_right_filled
            )

            binding.stepBullet.isVisible = !isCompleted
            binding.stepCheck.isVisible = isCompleted
        }
    }
}

data class OnboardingStepData(
    val type: OnboardingStepType,
    val title: String,
    val description: String?,
    val canSkip: Boolean,
    val isCompleted: (() -> Boolean),
    val openAction: (() -> Unit),
    val skipAction: (() -> Unit)? = null
)

enum class OnboardingStepType {
    LOGIN,
    NOTIFICATION_LISTENER,
    DKMA,
    CHOOSE_APPS,
    SEND_NOTIFICATIONS,
}