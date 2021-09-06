package com.arn.scrobble.pref

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.MainActivity
import com.arn.scrobble.Stuff
import com.arn.scrobble.db.*
import com.arn.scrobble.pref.JsonHelpers.readJson
import com.arn.scrobble.pref.JsonHelpers.writeJson
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ImExporter {
    private var writer: OutputStreamWriter? = null
    private var reader: InputStreamReader? = null
    private var pfd: ParcelFileDescriptor? = null
    private var context: Context? = null

    // DO not store longs, as they cannot be determined by JsonToken
    private val prefsToConsider = setOf(
        MainPrefs.PREF_MASTER,
        MainPrefs.CHANNEL_NOTI_SCROBBLING,
        MainPrefs.CHANNEL_NOTI_DIGEST_WEEKLY,
        MainPrefs.CHANNEL_NOTI_DIGEST_MONTHLY,
        MainPrefs.PREF_LOCKSCREEN_NOTI,
        MainPrefs.PREF_PIXEL_NP,
        MainPrefs.PREF_DELAY_SECS,
        MainPrefs.PREF_DELAY_PER,
        MainPrefs.PREF_NOW_PLAYING,
        MainPrefs.PREF_FETCH_AA,
        MainPrefs.PREF_LOCALE,
        MainPrefs.PREF_AUTO_DETECT,
        MainPrefs.PREF_SHOW_RECENTS_ALBUM,
        MainPrefs.PREF_THEME_PRIMARY,
        MainPrefs.PREF_THEME_SECONDARY,
        MainPrefs.PREF_THEME_BACKGROUND,
        MainPrefs.PREF_THEME_SAME_TONE,
        MainPrefs.PREF_THEME_RANDOM,
        MainPrefs.PREF_THEME_PALETTE_BG,
        MainPrefs.PREF_LOCALE,

        MainPrefs.PREF_WHITELIST,
        MainPrefs.PREF_BLACKLIST,
    )

    fun setOutputUri(context: Context, uri: Uri){
        pfd = context.contentResolver.openFileDescriptor(uri, "w")
        if (pfd == null){
            Stuff.log("pfd was null")
            return
        }
        val fos = FileOutputStream(pfd!!.fileDescriptor)
        writer = OutputStreamWriter(fos, "UTF-8")
        this.context = context
    }

    fun setInputUri(context: Context, uri: Uri){
        pfd = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd == null){
            Stuff.log("pfd was null")
            return
        }
        val fis = FileInputStream(pfd!!.fileDescriptor)
        reader = InputStreamReader(fis, "UTF-8")
        this.context = context
    }

    fun export(): Boolean {
        if (context == null || writer == null){
            throw IllegalArgumentException("ImExporter not inited")
        }
        val context = context!!
        var written = false
        try {
            JsonWriter(writer!!).use {
                it.apply {
                    setIndent("  ")

                    beginObject()
                    name("pano_version").value(BuildConfig.VERSION_CODE)
                    name("simple_edits").beginArray()
                    PanoDb.getDb(context)
                        .getSimpleEditsDao()
                        .all
                        .asReversed()
                        .forEach {
                            it.writeJson(this)
                        }
                    endArray()
                    name("regex_edits").beginArray()
                    PanoDb.getDb(context)
                        .getRegexEditsDao()
                        .all
                        .forEach {
                            it.writeJson(this)
                        }
                    endArray()
                    name("blocked_metadata").beginArray()
                    PanoDb.getDb(context)
                        .getBlockedMetadataDao()
                        .all
                        .asReversed()
                        .forEach {
                            it.writeJson(this)
                        }
                    endArray()

                    name("settings").beginObject()

                    MainPrefs(context).sharedPreferences.all.forEach { (prefKey, prefValue) ->
                        if (prefValue == null || prefKey !in prefsToConsider)
                            return@forEach

                        name(prefKey)
                        when (prefValue) {
                            is Boolean -> value(prefValue)
                            is Float -> value(prefValue)
                            is Int -> value(prefValue)
                            is Long -> value(prefValue)
                            is String -> value(prefValue)
                            is Set<*> -> {
                                beginArray()
                                prefValue.forEach { value(it as String) }
                                endArray()
                            }
                        }
                    }

                    endObject()

                    endObject()
                    written = true
                }
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
        return written
    }

    fun import(editsMode: Int, settings: Boolean): Boolean {
        if (context == null || reader == null){
            throw IllegalArgumentException("ImExporter not inited")
        }
        val context = context!!
        try {
            JsonReader(reader).use {
                it.apply {
                    beginObject()
                    var versionCode = -1
                    while (hasNext()) {
                        val name = nextName()
                        if (name in arrayOf("edits", "simple_edits", "regex_edits", "blocked_metadata") && editsMode != Stuff.EDITS_NOPE) {
                            when(name) {
                                "simple_edits",
                                "edits" -> {
                                    val dao = PanoDb.getDb(context).getSimpleEditsDao()
                                    if (editsMode == Stuff.EDITS_REPLACE_ALL)
                                        dao.nuke()
                                    val edits = mutableListOf<SimpleEdit>()
                                    beginArray()
                                    while (hasNext()) {
                                        edits += SimpleEdit().readJson(this)
                                    }
                                    endArray()

                                    if (editsMode == Stuff.EDITS_REPLACE_EXISTING || editsMode == Stuff.EDITS_REPLACE_ALL)
                                        dao.insert(edits)
                                    else if (editsMode == Stuff.EDITS_KEEP_EXISTING)
                                        dao.insertIgnore(edits)
                                }
                                "regex_edits" -> {
                                    val dao = PanoDb.getDb(context).getRegexEditsDao()
                                    if (editsMode == Stuff.EDITS_REPLACE_ALL)
                                        dao.nuke()
                                    val edits = mutableListOf<RegexEdit>()
                                    beginArray()
                                    while (hasNext()) {
                                        edits += RegexEdit().readJson(this)
                                    }
                                    endArray()

                                    val existingEdits = dao.all
                                    existingEdits.forEach { it._id = 0 }
                                    val deduplicatedEdits = (edits.toSet() - existingEdits.toSet()).toList()
                                    if (deduplicatedEdits.isNotEmpty()) {
                                        val offset = (dao.maxOrder ?: -1) + 1
                                        deduplicatedEdits.forEach { it.order += offset }
                                    }

                                    if (editsMode == Stuff.EDITS_REPLACE_EXISTING || editsMode == Stuff.EDITS_REPLACE_ALL)
                                        dao.insert(deduplicatedEdits)
                                    else if (editsMode == Stuff.EDITS_KEEP_EXISTING)
                                        dao.insertIgnore(deduplicatedEdits)
                                }
                                "blocked_metadata" -> {
                                    val dao = PanoDb.getDb(context).getBlockedMetadataDao()
                                    if (editsMode == Stuff.EDITS_REPLACE_ALL)
                                        dao.nuke()
                                    val blockedMetadata = mutableListOf<BlockedMetadata>()
                                    beginArray()
                                    while (hasNext()) {
                                        blockedMetadata += BlockedMetadata().readJson(this)
                                    }
                                    endArray()

                                    if (editsMode == Stuff.EDITS_REPLACE_EXISTING || editsMode == Stuff.EDITS_REPLACE_ALL)
                                        dao.insertLowerCase(blockedMetadata, ignore = false)
                                    else if (editsMode == Stuff.EDITS_KEEP_EXISTING)
                                        dao.insertLowerCase(blockedMetadata, ignore = true)
                                }
                            }


                        } else if (name == "settings" && settings) {
                            val prefs = MainPrefs(context).sharedPreferences.edit()
                            val settingsNamesInJson = mutableSetOf<String>()
                            beginObject()
                            while (hasNext()) {
                                val settingsName = nextName()
                                settingsNamesInJson += settingsName

                                if (settingsName !in prefsToConsider) {
                                    skipValue()
                                    continue
                                }

                                when(peek()) {
                                    JsonToken.BEGIN_ARRAY -> {
                                        val list = mutableSetOf<String>()
                                        beginArray()
                                        if (settingsName == MainPrefs.PREF_WHITELIST || settingsName == MainPrefs.PREF_BLACKLIST) {
                                            while (hasNext()) {
                                                val pkgName = nextString()
                                                try {
                                                    context.packageManager?.getPackageInfo(
                                                        pkgName,
                                                        0
                                                    )
                                                    list += pkgName
                                                } catch (e: PackageManager.NameNotFoundException) {
                                                    Stuff.log("$pkgName not installed")
                                                }
                                            }
                                        } else {
                                            while (hasNext()) {
                                                list += nextString()
                                            }
                                        }
                                        prefs.putStringSet(settingsName, list)
                                        endArray()
                                    }
                                    JsonToken.NUMBER -> {
                                        val numStr = nextString()
                                        if ('.' in numStr)
                                            prefs.putFloat(settingsName, numStr.toFloat())
                                        else
                                            prefs.putInt(settingsName, numStr.toInt())
                                    }
                                    JsonToken.STRING -> {
                                        prefs.putString(settingsName, nextString())
                                    }
                                    JsonToken.BOOLEAN -> {
                                        if (
                                            settingsName == MainPrefs.PREF_AUTO_DETECT && MainActivity.isTV ||
                                            settingsName == MainPrefs.PREF_PIXEL_NP && Build.MANUFACTURER.lowercase() != Stuff.MANUFACTURER_GOOGLE
                                        )
                                            skipValue()
                                        else
                                            prefs.putBoolean(settingsName, nextBoolean())
                                    }
                                    else -> skipValue()
                                }
                            }
                            endObject()
                            if (settingsNamesInJson.isNotEmpty()) {
                                val settingsToReset = prefsToConsider - settingsNamesInJson
                                settingsToReset.forEach {
                                    prefs.remove(it)
                                }
                            }
                            prefs.apply()
                        } else if (name == "pano_version")
                            versionCode = nextInt()
                        else
                            skipValue()
                    }
                    endObject()
                    if (versionCode == -1)
                        return false
                    return true
                }
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
        return false
    }

    fun close(){
        context = null
        try {
            writer?.close()
        } catch (e: Exception){
        }
        try {
            reader?.close()
        } catch (e: Exception){
        }
        try {
            pfd?.close()
        } catch (e: Exception){
        }
        writer = null
        reader = null
        pfd = null
    }
}