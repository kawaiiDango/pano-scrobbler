package com.arn.scrobble.db

enum class RegexMode {
    Replace,
    Extract,
    Block
}

fun RegexEdit.mode(): RegexMode {
    return if (blockPlayerAction != null) RegexMode.Block
    else if (replacement == null) RegexMode.Extract
    else RegexMode.Replace
}