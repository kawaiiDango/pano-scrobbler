package com.arn.scrobble.db

import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.Database



/**
 * Created by arn on 11/09/2017.
 */

@Database(entities = arrayOf(PendingScrobbles::class), version = 1)
abstract class PendingScrobblesDb : RoomDatabase() {
    abstract fun getDao(): PendingScrobblesDao
}
