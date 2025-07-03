package com.arn.scrobble.db

import androidx.room.RoomDatabaseConstructor

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object PanoDbConstructor : RoomDatabaseConstructor<PanoDb> {
    override fun initialize(): PanoDb
}