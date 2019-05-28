package com.arn.scrobble.pref

import android.content.ContentProviderClient
import android.content.ContentValues
import android.content.Context
import android.os.Build

/**
 * Multi Preference class
 *
 * - allows access to Shared Preferences across processes through a
 * Content Provider
 */
class MultiPreferences(private val name: String, private val context: Context) {

    constructor(context: Context) : this(context.packageName + "_preferences", context)

    private fun update(code:Int, key: String, value: Any) {
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

        if (client == null)
            client = context.contentResolver.acquireContentProviderClient(updateUri)

        client?.update(updateUri, contentValues, null, null)
    }

    private fun query(code:Int, key: String, defaultValue: Any?): Any? {
        var value = defaultValue
//        Stuff.log("query: $key $defaultValue $value")
        val queryUri = MultiPrefsProvider.createQueryUri(name, key, code)

        if (client == null)
            client = context.contentResolver.acquireContentProviderClient(queryUri)

        val cursor = client?.query(queryUri, null, null, null, defaultValue?.toString())

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
                        MultiPrefsProvider.CODE_SIZE -> cursor.getInt(colIndex)
                        else -> throw IllegalArgumentException("Invalid type")
                    }
        }
        cursor?.close()
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

    fun size() = query(MultiPrefsProvider.CODE_SIZE, "", 0) as Int

    fun remove(key: String) {
        val deleteUri = MultiPrefsProvider.createQueryUri(name, key, MultiPrefsProvider.CODE_INTEGER)
        context.contentResolver.delete(deleteUri, null, null)
    }

    fun clearPreferences() {
        val clearPrefsUri = MultiPrefsProvider.createQueryUri(name, "", MultiPrefsProvider.CODE_PREFS)
        context.contentResolver.delete(clearPrefsUri, null, null)
    }

    companion object {
        var client: ContentProviderClient? = null

        fun destroyClient() {
            if (client != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    client!!.close()
                else
                    client!!.release()
                client = null
            }
        }
    }
}
