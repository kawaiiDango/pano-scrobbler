package com.arn.scrobble.onboarding

import androidx.lifecycle.ViewModel

class OnboardingVM : ViewModel() {
    var selectedPosition = 0
    val skippedPositions = mutableSetOf<Int>()
}