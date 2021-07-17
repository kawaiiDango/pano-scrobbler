package com.arn.scrobble.pref

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.JsonReader
import android.util.JsonWriter
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.Main
import com.arn.scrobble.Stuff
import com.arn.scrobble.db.*
import com.arn.scrobble.pref.JsonHelpers.readJson
import com.arn.scrobble.pref.JsonHelpers.writeJson
import com.arn.scrobble.themes.ColorPatchUtils
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ImExporter {
    private var writer: OutputStreamWriter? = null
    private var reader: InputStreamReader? = null
    private var pfd: ParcelFileDescriptor? = null
    private var context: Context? = null

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
            Stuff.log("ImExporter not inited")
            return false
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
                    val pref = MultiPreferences(context)
                    name(Stuff.PREF_MASTER).value(pref.getBoolean(Stuff.PREF_MASTER, true))
                    name(Stuff.PREF_NOTIFICATIONS).value(pref.getBoolean(Stuff.PREF_NOTIFICATIONS, true))
                    name(Stuff.PREF_DIGEST_WEEKLY).value(pref.getBoolean(Stuff.PREF_DIGEST_WEEKLY, true))
                    name(Stuff.PREF_DIGEST_MONTHLY).value(pref.getBoolean(Stuff.PREF_DIGEST_MONTHLY, true))
                    name(Stuff.PREF_LOCKSCREEN_NOTI).value(pref.getBoolean(Stuff.PREF_LOCKSCREEN_NOTI, false))
                    name(Stuff.PREF_PIXEL_NP).value(pref.getBoolean(Stuff.PREF_AUTO_DETECT, true))
                    name(Stuff.PREF_DELAY_SECS).value(pref.getInt(Stuff.PREF_DELAY_SECS, Stuff.PREF_DELAY_SECS_DEFAULT))
                    name(Stuff.PREF_DELAY_PER).value(pref.getInt(Stuff.PREF_DELAY_PER, Stuff.PREF_DELAY_PER_DEFAULT))
                    name(Stuff.PREF_NOW_PLAYING).value(pref.getBoolean(Stuff.PREF_NOW_PLAYING, true))
                    name(Stuff.PREF_FETCH_AA).value(pref.getBoolean(Stuff.PREF_FETCH_AA, false))
                    name(Stuff.PREF_AUTO_DETECT).value(pref.getBoolean(Stuff.PREF_AUTO_DETECT, true))
                    name(Stuff.PREF_SHOW_RECENTS_ALBUM).value(pref.getBoolean(Stuff.PREF_SHOW_RECENTS_ALBUM, false))
                    name(Stuff.PREF_THEME_PRIMARY).value(pref.getString(Stuff.PREF_THEME_PRIMARY, ColorPatchUtils.primaryDefault))
                    name(Stuff.PREF_THEME_SECONDARY).value(pref.getString(Stuff.PREF_THEME_SECONDARY, ColorPatchUtils.secondaryDefault))
                    name(Stuff.PREF_THEME_BACKGROUND).value(pref.getString(Stuff.PREF_THEME_BACKGROUND, ColorPatchUtils.backgroundDefault))
                    name(Stuff.PREF_THEME_SAME_TONE).value(pref.getBoolean(Stuff.PREF_THEME_SAME_TONE, false))
                    name(Stuff.PREF_THEME_RANDOM).value(pref.getBoolean(Stuff.PREF_THEME_RANDOM, false))
                    name(Stuff.PREF_WHITELIST).beginArray()
                    pref.getStringSet(Stuff.PREF_WHITELIST, setOf())
                            .forEach {
                                value(it)
                            }
                    endArray()
                    name(Stuff.PREF_BLACKLIST).beginArray()
                    pref.getStringSet(Stuff.PREF_BLACKLIST, setOf())
                            .forEach {
                                value(it)
                            }
                    endArray()
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
            Stuff.log("ImExporter not inited")
            return false
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
                            val pref = MultiPreferences(context)
                            beginObject()
                            while (hasNext()) {
                                val settingsName = nextName()
                                if (settingsName == Stuff.PREF_WHITELIST || settingsName == Stuff.PREF_BLACKLIST) {
                                    val list = mutableSetOf<String>()
                                    beginArray()
                                    while (hasNext()) {
                                        val pkgName = nextString()
                                        try {
                                            context.packageManager?.getPackageInfo(pkgName, 0)
                                            list += pkgName
                                        } catch (e: PackageManager.NameNotFoundException) {
                                            Stuff.log("$pkgName not installed")
                                        }
                                    }
                                    pref.putStringSet(settingsName, list)
                                    endArray()
                                } else if (settingsName == Stuff.PREF_DELAY_SECS || settingsName == Stuff.PREF_DELAY_PER) {
                                    pref.putInt(settingsName, nextInt())
                                } else {
                                    if (settingsName == Stuff.PREF_AUTO_DETECT && Main.isTV)
                                        skipValue()
                                    else if (settingsName == Stuff.PREF_PIXEL_NP) {
                                        try {
                                            context.packageManager?.getPackageInfo(Stuff.PACKAGE_PIXEL_NP_R, 0)
                                            pref.putBoolean(settingsName, nextBoolean())
                                        } catch (e: PackageManager.NameNotFoundException) {
                                            skipValue()
                                        }
                                    } else if (settingsName in arrayOf(
                                            Stuff.PREF_THEME_PRIMARY,
                                            Stuff.PREF_THEME_SECONDARY,
                                            Stuff.PREF_THEME_BACKGROUND
                                    ))
                                        pref.putString(settingsName, nextString())
                                    else if (settingsName in arrayOf(Stuff.PREF_PRO_STATUS)) {

                                    } else
                                        pref.putBoolean(settingsName, nextBoolean())
                                }
                            }
                            endObject()
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