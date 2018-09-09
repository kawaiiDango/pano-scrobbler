package com.arn.scrobble.pref

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences

/**
 * Multi Preference class
 *
 * - allows access to Shared Preferences across processes through a
 * Content Provider
 */
class MultiPreferences(private val name: String, private val context: Context) {

    constructor(context: Context) : this(context.packageName + "_preferences", context)

    private var pref: SharedPreferences? = null

    var skipContentProvider = false
        set(value) {
            if (value && pref == null)
                pref = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            else if (!value && pref != null)
                pref = null
            field = value
        }

    private fun update(code:Int, key: String, value: Any) {
        if (skipContentProvider) {
            val editor = pref!!.edit()
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value as Set<String>)
                else -> throw IllegalArgumentException("Invalid type")
            }
            editor.apply()
        } else {
            val contentValues = ContentValues()

            when (value) {
                is String -> contentValues.put(MultiPrefsProvider.VALUE, value)
                is Int -> contentValues.put(MultiPrefsProvider.VALUE, value)
                is Long -> contentValues.put(MultiPrefsProvider.VALUE, value)
                is Boolean -> contentValues.put(MultiPrefsProvider.VALUE, value)
                is Float -> contentValues.put(MultiPrefsProvider.VALUE, value)
                is Set<*> -> contentValues.put(MultiPrefsProvider.VALUE, value.joinToString())
                else -> throw IllegalArgumentException("Invalid type")
            }
            val updateUri = MultiPrefsProvider.createQueryUri(name, key, code)
            context.contentResolver.update(updateUri, contentValues, null, null)
        }
    }

    private fun query(code:Int, key: String, defaultValue: Any?): Any? {
        var value = defaultValue
//        Stuff.log("query: $key $defaultValue $value")
        if (skipContentProvider) {
            val pref = pref!!
            value = when (code) {
                        MultiPrefsProvider.CODE_STRING -> pref.getString(key, defaultValue as String?)
                        MultiPrefsProvider.CODE_INTEGER -> pref.getInt(key, defaultValue as Int)
                        MultiPrefsProvider.CODE_LONG -> pref.getLong(key, defaultValue as Long)
                        MultiPrefsProvider.CODE_BOOLEAN -> pref.getBoolean(key, defaultValue as Boolean)
                        MultiPrefsProvider.CODE_FLOAT -> pref.getFloat(key, defaultValue as Float)
                        MultiPrefsProvider.CODE_STRING_SET -> pref.getStringSet(key, defaultValue as Set<String>)
                        else -> throw IllegalArgumentException("Invalid type")
                    }
        } else {
            val queryUri = MultiPrefsProvider.createQueryUri(name, key, code)
            val cursor = context.contentResolver.query(queryUri, null, null, null, defaultValue?.toString())
            if (cursor != null && cursor.moveToFirst()) {
                val colIndex = cursor.getColumnIndexOrThrow(MultiPrefsProvider.VALUE)
                value = when (code) {
                            MultiPrefsProvider.CODE_STRING -> cursor.getString(colIndex)
                            MultiPrefsProvider.CODE_INTEGER -> cursor.getInt(colIndex)
                            MultiPrefsProvider.CODE_LONG -> cursor.getLong(colIndex)
                            MultiPrefsProvider.CODE_BOOLEAN -> cursor.getInt(colIndex) == 1
                            MultiPrefsProvider.CODE_FLOAT -> cursor.getFloat(colIndex)
                            MultiPrefsProvider.CODE_STRING_SET -> {
                                val str = cursor.getString(colIndex)
                                if (str.isEmpty())
                                    setOf()
                                else
                                    str.split(", ").toSet()
                            }
                            else -> throw IllegalArgumentException("Invalid type")
                        }
            }
        cursor?.close()
        }

        return value
    }

    fun putString(key: String, value: String) = update(MultiPrefsProvider.CODE_STRING, key, value)
    fun getString(key: String, defaultValue: String?) = query(MultiPrefsProvider.CODE_STRING, key, defaultValue) as String?

    fun putInt(key: String, value: Int) = update(MultiPrefsProvider.CODE_INTEGER, key, value)
    fun getInt(key: String, defaultValue: Int) = query(MultiPrefsProvider.CODE_INTEGER, key, defaultValue) as Int

    fun putLong(key: String, value: Long) = update(MultiPrefsProvider.CODE_LONG, key, value)
    fun getLong(key: String, defaultValue: Long) = query(MultiPrefsProvider.CODE_LONG, key, defaultValue) as Long

    fun putBoolean(key: String, value: Boolean) = update(MultiPrefsProvider.CODE_BOOLEAN, key, value)
    fun getBoolean(key: String, defaultValue: Boolean) = query(MultiPrefsProvider.CODE_BOOLEAN, key, defaultValue) as Boolean

    fun putFloat(key: String, value: Float) = update(MultiPrefsProvider.CODE_FLOAT, key, value)
    fun getFloat(key: String, defaultValue: Float) = query(MultiPrefsProvider.CODE_FLOAT, key, defaultValue) as Float

    fun putStringSet(key: String, value: Set<String>?) = update(MultiPrefsProvider.CODE_STRING_SET, key, value ?: setOf<String>())
    fun getStringSet(key: String, defaultValue: Set<String>?) = query(MultiPrefsProvider.CODE_STRING_SET, key, defaultValue) as Set<String>

    fun remove(key: String) {
        val deleteUri = MultiPrefsProvider.createQueryUri(name, key, MultiPrefsProvider.CODE_INTEGER)
        context.contentResolver.delete(deleteUri, null, null)
    }

    fun clearPreferences() {
        val clearPrefsUri = MultiPrefsProvider.createQueryUri(name, "", MultiPrefsProvider.CODE_PREFS)
        context.contentResolver.delete(clearPrefsUri, null, null)
    }
}
