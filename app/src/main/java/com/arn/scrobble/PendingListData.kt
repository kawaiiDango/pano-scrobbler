package com.arn.scrobble

import com.arn.scrobble.pending.db.PendingLove
import com.arn.scrobble.pending.db.PendingScrobble

class PendingListData {
    var psCount = 0
    lateinit var psList: List<PendingScrobble>
    var plCount = 0
    lateinit var plList: List<PendingLove>
}