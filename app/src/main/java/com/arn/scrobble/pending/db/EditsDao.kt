package com.arn.scrobble.pending.db

import androidx.room.*


/**
 * Created by arn on 11/09/2017.
 */
private const val tableName = "edits"

@Dao
interface EditsDao {
    @get:Query("SELECT * FROM $tableName")
    val all: List<Edit>

    @Query("SELECT * FROM $tableName WHERE hash =:hash")
    fun find(hash:String): Edit?

    @get:Query("SELECT count(*) FROM $tableName")
    val count: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg e: Edit)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(e: Edit)

    @Transaction
    fun upsert(e:Edit){
        if (find(e.hash) != null)
            update(e)
        else
            insert(e)
    }

    @Delete
    fun delete(e: Edit)
}
