package com.arn.scrobble.db

import android.arch.persistence.room.*


/**
 * Created by arn on 11/09/2017.
 */

@Dao
interface PendingScrobblesDao {
    @get:Query("SELECT * FROM PendingScrobbles")
    val all: List<PendingScrobbles>

    @Query("SELECT * FROM PendingScrobbles WHERE uid IN (:userIds)")
    fun loadAllNotAc(userIds: IntArray): List<PendingScrobbles>

    @Query("SELECT * FROM PendingScrobbles WHERE first_name LIKE :first AND " + "last_name LIKE :last LIMIT 1")
    fun loadAllPending(first: String, last: String): PendingScrobbles

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg ps: PendingScrobbles)

    @Delete
    fun delete(ps: PendingScrobbles)
}
