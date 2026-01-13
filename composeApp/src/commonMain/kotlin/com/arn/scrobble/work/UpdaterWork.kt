package com.arn.scrobble.work

expect object UpdaterWork : CommonWork {
    fun schedule(force: Boolean)
}