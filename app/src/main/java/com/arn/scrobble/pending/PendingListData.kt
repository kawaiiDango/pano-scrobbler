package com.arn.scrobble.pending

import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble

class PendingListData {
    var psCount = 0
    lateinit var psList: List<PendingScrobble>
    var plCount = 0
    lateinit var plList: List<PendingLove>
}