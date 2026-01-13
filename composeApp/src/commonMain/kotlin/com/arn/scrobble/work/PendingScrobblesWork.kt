package com.arn.scrobble.work

expect object PendingScrobblesWork : CommonWork {
    fun schedule(force: Boolean)
}