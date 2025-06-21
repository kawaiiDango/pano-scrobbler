package com.arn.scrobble.db

enum class RegexMode {
    ReplaceFirst,
    ReplaceAll,
    Extract,
    Block
}

fun RegexEdit.mode(): RegexMode {
    return if (blockPlayerAction != null) RegexMode.Block
    else if (replacement == null) RegexMode.Extract
    else if (replacement.replaceAll) RegexMode.ReplaceAll
    else RegexMode.ReplaceFirst
}