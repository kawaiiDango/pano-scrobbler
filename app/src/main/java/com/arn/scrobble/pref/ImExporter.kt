package com.arn.scrobble.pref

import com.arn.scrobble.BuildConfig
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.Converters
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.main.App
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

class ImExporter {
    private val db = PanoDb.db
    private val prefs = App.prefs
    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            prettyPrint = true
            encodeDefaults = true
        }
    }

    fun export(writer: OutputStream): Boolean {

        val appPrefs = AppPrefs(
            pano_version = BuildConfig.VERSION_CODE,
            simple_edits = db.getSimpleEditsDao().all().asReversed(),
            blocked_metadata = db.getBlockedMetadataDao().all().asReversed(),
            regex_edits = db.getRegexEditsDao().all(),
            scrobble_sources = null,
            settings = MainPrefs.MainPrefsPublic()
        )

        // write to file
        return try {
            json.encodeToStream(appPrefs, writer)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            writer.close()
        }


    }

    fun exportPrivateData(writer: OutputStream): Boolean {
        val appPrefsPrivate = AppPrefs(
            pano_version = BuildConfig.VERSION_CODE,
            scrobble_sources = db.getScrobbleSourcesDao().all().asReversed(),
            settings = null,
            simple_edits = null,
            regex_edits = null,
            blocked_metadata = null,
        )

        // write to file
        return try {
            json.encodeToStream(appPrefsPrivate, writer)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            writer.close()
        }
    }

    fun import(reader: InputStream, editsMode: EditsMode, settings: Boolean): Boolean {
        val appPrefs: AppPrefs
        try {
            appPrefs = json.decodeFromStream<AppPrefs>(reader)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        if (settings && appPrefs.settings != null)
            prefs.fromMainPrefsPublic(appPrefs.settings)

        if (editsMode == EditsMode.EDITS_NOPE)
            return true

        if (editsMode == EditsMode.EDITS_REPLACE_ALL) {
            if (appPrefs.simple_edits != null)
                db.getSimpleEditsDao().nuke()
            if (appPrefs.regex_edits != null)
                db.getRegexEditsDao().nuke()
            if (appPrefs.blocked_metadata != null)
                db.getBlockedMetadataDao().nuke()
            if (appPrefs.scrobble_sources != null)
                db.getScrobbleSourcesDao().nuke()
        }

        appPrefs.regex_edits?.forEach {
            if (it.fieldCompat != null) {
                it.fields = Converters.fromCommaSeperatedString(it.fieldCompat)
                it.fieldCompat = null
            }
        }

        if (editsMode == EditsMode.EDITS_REPLACE_ALL || editsMode == EditsMode.EDITS_REPLACE_EXISTING) {
            appPrefs.simple_edits?.let { db.getSimpleEditsDao().insert(it) }
            appPrefs.regex_edits?.let { db.getRegexEditsDao().insert(it) }
            appPrefs.blocked_metadata?.let { db.getBlockedMetadataDao().insert(it) }
        } else if (editsMode == EditsMode.EDITS_KEEP_EXISTING) {
            appPrefs.simple_edits?.let { db.getSimpleEditsDao().insertIgnore(it) }
            appPrefs.regex_edits?.let { db.getRegexEditsDao().insertIgnore(it) }
            appPrefs.blocked_metadata?.let { db.getBlockedMetadataDao().insertIgnore(it) }
        }
        appPrefs.scrobble_sources?.let { db.getScrobbleSourcesDao().insert(it) }

        reader.close()

        return true
    }
}

@Serializable
private data class AppPrefs(
    val pano_version: Int,

    @JsonNames("edits")
    val simple_edits: List<SimpleEdit>?,
    val regex_edits: List<RegexEdit>?,
    val blocked_metadata: List<BlockedMetadata>?,
    val scrobble_sources: List<ScrobbleSource>?,
    val settings: MainPrefs.MainPrefsPublic?,
)

enum class EditsMode {
    EDITS_NOPE,
    EDITS_REPLACE_ALL,
    EDITS_REPLACE_EXISTING,
    EDITS_KEEP_EXISTING,
}