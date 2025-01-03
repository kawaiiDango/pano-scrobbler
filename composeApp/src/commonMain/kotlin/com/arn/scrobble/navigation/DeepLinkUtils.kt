package com.arn.scrobble.navigation


expect fun handleNavigationFromInfoScreen(
    route: PanoRoute,
    usingInDialogActivity: Boolean,
    navigate: (PanoRoute) -> Unit,
)