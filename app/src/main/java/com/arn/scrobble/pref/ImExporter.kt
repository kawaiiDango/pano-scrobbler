package com.arn.scrobble.pref

import android.content.pm.PackageManager
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.Stuff
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.BlockedMetadataDao.Companion.insertLowerCase
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.pref.JsonHelpers.readJson
import com.arn.scrobble.pref.JsonHelpers.writeJson
import java.io.Closeable
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ImExporter : Closeable {
    private var writer: OutputStreamWriter? = null
    private var reader: InputStreamReader? = null
    private var pfd: ParcelFileDescriptor? = null
    private val context = App.context

    // DO not store longs, as they cannot be determined by JsonToken
    private val prefsToConsider = setOf(
        MainPrefs.PREF_MASTER,
        MainPrefs.CHANNEL_NOTI_SCROBBLING,
        MainPrefs.CHANNEL_NOTI_DIGEST_WEEKLY,
        MainPrefs.CHANNEL_NOTI_DIGEST_MONTHLY,
        MainPrefs.PREF_LOCKSCREEN_NOTI,
        MainPrefs.PREF_DELAY_SECS,
        MainPrefs.PREF_DELAY_PER,
        MainPrefs.PREF_NOW_PLAYING,
        MainPrefs.PREF_FETCH_AA,
        MainPrefs.PREF_FETCH_ALBUM,
        MainPrefs.PREF_LOCALE,
        MainPrefs.PREF_AUTO_DETECT,
        MainPrefs.PREF_SHOW_RECENTS_ALBUM,
        MainPrefs.PREF_SHOW_SCROBBLE_SOURCES,
        MainPrefs.PREF_THEME_PRIMARY,
        MainPrefs.PREF_THEME_SECONDARY,
        MainPrefs.PREF_THEME_BACKGROUND,
        MainPrefs.PREF_THEME_RANDOM,
        MainPrefs.PREF_THEME_DAY_NIGHT,
        MainPrefs.PREF_THEME_TINT_BG,
        MainPrefs.PREF_THEME_DYNAMIC,
        MainPrefs.PREF_SEARCH_IN_SOURCE,
        MainPrefs.PREF_LOCALE,
        MainPrefs.PREF_SCROBBLE_SPOTIFY_REMOTE,

        MainPrefs.PREF_ALLOWED_PACKAGES,
        MainPrefs.PREF_BLOCKED_PACKAGES,
    )

    fun setOutputUri(uri: Uri) {
        pfd = context.contentResolver.openFileDescriptor(uri, "w")
        if (pfd == null) {
            Stuff.log("pfd was null")
            return
        }
        val fos = FileOutputStream(pfd!!.fileDescriptor)
        writer = OutputStreamWriter(fos, "UTF-8")
    }

    fun setInputUri(uri: Uri) {
        pfd = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd == null) {
            Stuff.log("pfd was null")
            return
        }
        val fis = FileInputStream(pfd!!.fileDescriptor)
        reader = InputStreamReader(fis, "UTF-8")
    }

    fun export(): Boolean {
        if (writer == null) {
            throw IllegalArgumentException("ImExporter not inited")
        }
        var written = false
        try {
            JsonWriter(writer!!).use {
                it.apply {
                    setIndent("  ")

                    beginObject()
                    name("pano_version").value(BuildConfig.VERSION_CODE)
                    name("simple_edits").beginArray()
                    PanoDb.db
                        .getSimpleEditsDao()
                        .all
                        .asReversed()
                        .forEach {
                            it.writeJson(this)
                        }
                    endArray()
                    name("regex_edits").beginArray()
                    PanoDb.db
                        .getRegexEditsDao()
                        .all
                        .forEach {
                            it.writeJson(this)
                        }
                    endArray()
                    name("blocked_metadata").beginArray()
                    PanoDb.db
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return written
    }

    fun exportPrivateData(): Boolean {
        if (writer == null) {
            throw IllegalArgumentException("ImExporter not inited")
        }
        var written = false
        try {
            JsonWriter(writer!!).use {
                it.apply {
                    setIndent("  ")

                    beginObject()
                    name("pano_version").value(BuildConfig.VERSION_CODE)
                    name("scrobble_sources").beginArray()
                    PanoDb.db.getScrobbleSourcesDao().all.forEach { scrobbleSource ->
                        scrobbleSource.writeJson(this)
                    }
                    endArray()
                    endObject()
                }
            }
            written = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return written
    }

    fun import(editsMode: Int, settings: Boolean): Boolean {
        if (reader == null) {
            throw IllegalArgumentException("ImExporter not inited")
        }
        try {
            JsonReader(reader).use {
                it.apply {
                    beginObject()

                    if (!(nextName() == "pano_version" && nextInt() > 0))
                        return false

                    while (hasNext()) {
                        val name = nextName()

                        if (name in arrayOf(
                                "edits",
                                "simple_edits",
                                "regex_edits",
                                "blocked_metadata",
                                "scrobble_sources",
                            )
                        ) {
                            if (editsMode == Stuff.EDITS_NOPE) {
                                skipValue()
                                continue
                            }

                            when (name) {
                                "simple_edits",
                                "edits" -> {
                                    val dao = PanoDb.db.getSimpleEditsDao()
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
                                    val dao = PanoDb.db.getRegexEditsDao()
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
                                    val deduplicatedEdits =
                                        (edits.toSet() - existingEdits.toSet()).toList()
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
                                    val dao = PanoDb.db.getBlockedMetadataDao()
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

                                "scrobble_sources" -> {
                                    val dao = PanoDb.db.getScrobbleSourcesDao()
                                    if (editsMode == Stuff.EDITS_REPLACE_ALL)
                                        dao.nuke()
                                    val scrobbleSources = mutableListOf<ScrobbleSource>()
                                    beginArray()
                                    while (hasNext()) {
                                        scrobbleSources += ScrobbleSource(0, 0, "").readJson(this)
                                    }
                                    endArray()

                                    dao.insert(scrobbleSources)
                                }
                            }

                        } else if (name == "settings") {

                            if (!settings) {
                                skipValue()
                                continue
                            }

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

                                when (peek()) {
                                    JsonToken.BEGIN_ARRAY -> {
                                        val list = mutableSetOf<String>()
                                        beginArray()
                                        if (settingsName == MainPrefs.PREF_ALLOWED_PACKAGES || settingsName == MainPrefs.PREF_BLOCKED_PACKAGES) {
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
                                        if (settingsName == MainPrefs.PREF_AUTO_DETECT && Stuff.isTv)
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
                        }
                    }
                    endObject()
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    override fun close() {
        try {
            writer?.close()
        } catch (e: Exception) {
        }
        try {
            reader?.close()
        } catch (e: Exception) {
        }
        try {
            pfd?.close()
        } catch (e: Exception) {
        }
    }
}