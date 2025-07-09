package com.arn.scrobble.utils

data class CmdlineArgs(
    val minimized: Boolean = false,
    val dataDir: String? = null,
    val noUpdateCheck: Boolean = false,
    val automationCommand: String?,
    val automationArg: String?
)