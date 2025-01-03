package com.arn.scrobble.navigation


actual fun handleNavigationFromInfoScreen(
    route: PanoRoute,
    usingInDialogActivity: Boolean,
    navigate: (PanoRoute) -> Unit,
) {
    navigate(route)
}