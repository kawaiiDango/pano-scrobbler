package com.arn.scrobble.work

expect object IndexerWork : CommonWork {
    fun schedule(full: Boolean)
}