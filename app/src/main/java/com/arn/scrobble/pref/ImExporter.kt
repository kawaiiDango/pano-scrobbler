package com.arn.scrobble.pref

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.JsonReader
import android.util.JsonWriter
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.Main
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.pending.db.Edit
import com.arn.scrobble.pending.db.PendingScrobblesDb
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ImExporter {
    private var writer: OutputStreamWriter? = null
    private var reader: InputStreamReader? = null
    private var context: Context? = null

    fun setOutputUri(context: Context, uri: Uri){
        val pfd = context.contentResolver.openFileDescriptor(uri, "w")
        if (pfd == null){
            Stuff.log("pfd was null")
            return
        }
        val fos = FileOutputStream(pfd.fileDescriptor)
        writer = OutputStreamWriter(fos, "UTF-8")
        this.context = context
    }

    fun setInputUri(context: Context, uri: Uri){
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd == null){
            Stuff.log("pfd was null")
            return
        }
        val fis = FileInputStream(pfd.fileDescriptor)
        reader = InputStreamReader(fis, "UTF-8")
        this.context = context
    }

    fun export(): Boolean {
        if (context == null || writer == null){
            Stuff.log("ImExporter not inited")
            return false
        }
        val context = context!!
        try {
            JsonWriter(writer!!).use {
                it.apply {
                    setIndent("  ")

                    beginObject()
                    name("pano_version").value(BuildConfig.VERSION_CODE)
                    name("edits").beginArray()
                    val edits = PendingScrobblesDb.getDb(context).getEditsDao().all
                    edits.forEach {
                        beginObject()
                        name("hash").value(it.hash)
                        name("track").value(it.track)
                        name("album").value(it.album)
                        name("albumArtist").value(it.albumArtist)
                        name("artist").value(it.artist)
                        endObject()
                    }
                    endArray()

                    name("settings").beginObject()
                    val pref = MultiPreferences(context)
                    name(Stuff.PREF_MASTER).value(pref.getBoolean(Stuff.PREF_MASTER, true))
                    name(Stuff.PREF_NOTIFICATIONS).value(pref.getBoolean(Stuff.PREF_NOTIFICATIONS, true))
                    name(Stuff.PREF_PIXEL_NP).value(pref.getBoolean(Stuff.PREF_AUTO_DETECT, true))
                    name(Stuff.PREF_AUTO_DETECT).value(pref.getBoolean(Stuff.PREF_AUTO_DETECT, true))
                    name(Stuff.PREF_DELAY_SECS).value(pref.getInt(Stuff.PREF_DELAY_SECS, Stuff.PREF_DELAY_SECS_DEFAULT))
                    name(Stuff.PREF_DELAY_PER).value(pref.getInt(Stuff.PREF_DELAY_PER, Stuff.PREF_DELAY_PER_DEFAULT))
                    name(Stuff.PREF_NOW_PLAYING).value(pref.getBoolean(Stuff.PREF_NOW_PLAYING, true))
                    name(Stuff.PREF_AUTO_DETECT).value(pref.getBoolean(Stuff.PREF_AUTO_DETECT, true))
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
                    return true
                }
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
        return false
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
                        if (name == "edits" && editsMode != Stuff.EDITS_NOPE) {
                            val editsDao = PendingScrobblesDb.getDb(context).getEditsDao()
                            if (editsMode == Stuff.EDITS_REPLACE_ALL)
                                editsDao.nuke()
                            beginArray()
                            while (hasNext()) {
                                beginObject()
                                val edit = Edit()
                                while (hasNext()) {
                                    val editName = nextName()
                                    val editVal = nextString()
                                    when (editName) {
                                        "hash" -> edit.hash = editVal
                                        "track" -> edit.track = editVal
                                        "album" -> edit.album = editVal
                                        "albumArtist" -> edit.albumArtist = editVal
                                        "artist" -> edit.artist = editVal
                                        else -> return false
                                    }
                                    if (editsMode == Stuff.EDITS_REPLACE_EXISTING || editsMode == Stuff.EDITS_REPLACE_ALL)
                                        editsDao.insert(edit)
                                    else if (editsMode == Stuff.EDITS_KEEP_EXISTING)
                                        editsDao.insertIgnore(edit)
                                }
                                endObject()
                            }
                            endArray()
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
        writer = null
        reader = null
    }
}