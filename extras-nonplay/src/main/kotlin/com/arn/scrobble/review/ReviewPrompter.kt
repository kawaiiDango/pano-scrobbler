package com.arn.scrobble.review

import kotlinx.coroutines.flow.Flow

class ReviewPrompter(
    lastCheckTime: Flow<Long>,
    setLastCheckTime: suspend (Long) -> Unit,
) : BaseReviewPrompter(lastCheckTime, setLastCheckTime)