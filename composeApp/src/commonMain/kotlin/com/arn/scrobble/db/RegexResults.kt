package com.arn.scrobble.db

data class RegexResults(
    val fieldsMatched: Map<String, Set<RegexEdit>>,
    val blockPlayerAction: BlockPlayerAction?,
) {
    val isEdit = fieldsMatched.values.any { it.isNotEmpty() } && blockPlayerAction == null
    val isBlock = fieldsMatched.values.any { it.isNotEmpty() } && blockPlayerAction != null
}
