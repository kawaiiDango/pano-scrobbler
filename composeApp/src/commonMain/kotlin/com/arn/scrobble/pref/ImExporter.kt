package com.arn.scrobble.pref

import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.RegexEditsDao.Companion.import
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToStream
import java.io.OutputStream

class ImExporter {
    private val db = PanoDb.db
    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            prettyPrint = true
            encodeDefaults = true
        }
    }

    suspend fun export(writer: OutputStream): Boolean {
        val exportData = ExportData(
            pano_version = BuildKonfig.VER_CODE,
            simple_edits = db.getSimpleEditsDao().allFlow().first().asReversed(),
            blocked_metadata = db.getBlockedMetadataDao().allFlow().first().asReversed(),
            regex_rules = db.getRegexEditsDao().allFlow().first(),
            scrobble_sources = null,
            settings = PlatformStuff.mainPrefs.data.first().toPublicPrefs()
        )

        // write to file
        return try {
            json.encodeToStream(ExportDataSerializer, exportData, writer)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            withContext(Dispatchers.IO) {
                writer.close()
            }
        }

    }

    suspend fun exportPrivateData(writer: OutputStream): Boolean {
        val exportDataPrivate = ExportData(
            pano_version = BuildKonfig.VER_CODE,
            scrobble_sources = db.getScrobbleSourcesDao().all().asReversed(),
            settings = null,
            simple_edits = null,
            regex_rules = null,
            blocked_metadata = null,
        )

        // write to file
        return try {
            json.encodeToStream(ExportDataSerializer, exportDataPrivate, writer)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            withContext(Dispatchers.IO) {
                writer.close()
            }
        }
    }

    suspend fun import(jsonText: String, editsMode: EditsMode, settings: Boolean): Boolean {
        val exportData: ExportData = try {
            json.decodeFromString(jsonText)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        if (settings && exportData.settings != null) {
            PlatformStuff.mainPrefs.updateData {
                it.updateFromPublicPrefs(exportData.settings)
            }
        }

        if (editsMode == EditsMode.EDITS_NOPE) return true

        if (editsMode == EditsMode.EDITS_REPLACE_ALL) {
            if (exportData.simple_edits != null)
                db.getSimpleEditsDao().nuke()
            if (exportData.regex_rules != null || exportData.regex_edits_legacy != null)
                db.getRegexEditsDao().nuke()
            if (exportData.blocked_metadata != null)
                db.getBlockedMetadataDao().nuke()
            if (exportData.scrobble_sources != null)
                db.getScrobbleSourcesDao().nuke()
        }

        var migratedRegexEdits: List<RegexEdit>?

        if (exportData.regex_edits_legacy != null) {
            migratedRegexEdits = mutableListOf()

            exportData.regex_edits_legacy.forEach {
                if (it.preset != null) return@forEach

                if (it.extractionPatterns == null) {
                    val fields = it.fields?.map { field ->
                        val f = if (field == "albumartist") "albumArtist" else field
                        RegexEdit.Field.valueOf(f)
                    }?.toSet() ?: emptySet()

                    if (fields.isEmpty()) return@forEach

                    for (field in fields) {

                        migratedRegexEdits += RegexEdit(
                            name = it.name ?: "Untitled",
                            order = it.order,
                            search = RegexEdit.SearchPatterns(
                                searchTrack = it.pattern?.takeIf { field == RegexEdit.Field.track }
                                    .orEmpty(),
                                searchAlbum = it.pattern?.takeIf { field == RegexEdit.Field.album }
                                    .orEmpty(),
                                searchArtist = it.pattern?.takeIf { field == RegexEdit.Field.artist }
                                    .orEmpty(),
                                searchAlbumArtist = it.pattern?.takeIf { field == RegexEdit.Field.albumArtist }
                                    .orEmpty(),
                            ),

                            replacement = RegexEdit.ReplacementPatterns(
                                replacementTrack = it.replacement.takeIf { field == RegexEdit.Field.track }
                                    .orEmpty(),
                                replacementAlbum = it.replacement.takeIf { field == RegexEdit.Field.album }
                                    .orEmpty(),
                                replacementArtist = it.replacement.takeIf { field == RegexEdit.Field.artist }
                                    .orEmpty(),
                                replacementAlbumArtist = it.replacement.takeIf { field == RegexEdit.Field.albumArtist }
                                    .orEmpty(),
                                replaceAll = it.replaceAll,
                            ),
                            appIds = it.packages ?: emptySet(),
                            caseSensitive = it.caseSensitive,
                        )
                    }
                } else {
                    migratedRegexEdits += RegexEdit(
                        name = it.name ?: "Untitled",
                        order = it.order,
                        search = RegexEdit.SearchPatterns(
                            searchTrack = it.extractionPatterns.extractionTrack,
                            searchAlbum = it.extractionPatterns.extractionAlbum,
                            searchArtist = it.extractionPatterns.extractionArtist,
                            searchAlbumArtist = it.extractionPatterns.extractionAlbumArtist,
                        ),
                        appIds = it.packages ?: emptySet(),
                        caseSensitive = it.caseSensitive,
                    )

                }
            }
        } else
            migratedRegexEdits = null

        val regexEdits = migratedRegexEdits ?: exportData.regex_rules

        when (editsMode) {
            EditsMode.EDITS_REPLACE_ALL, EditsMode.EDITS_REPLACE_EXISTING -> {
                exportData.simple_edits?.let { db.getSimpleEditsDao().insert(it) }
                regexEdits?.let { db.getRegexEditsDao().import(it) }
                exportData.blocked_metadata?.let { db.getBlockedMetadataDao().insert(it) }
            }

            EditsMode.EDITS_KEEP_EXISTING -> {
                exportData.simple_edits?.let { db.getSimpleEditsDao().insertIgnore(it) }
                regexEdits?.let { db.getRegexEditsDao().import(it) }
                exportData.blocked_metadata?.let { db.getBlockedMetadataDao().insertIgnore(it) }
            }
        }
        exportData.scrobble_sources?.let { db.getScrobbleSourcesDao().insert(it) }

        return true
    }
}

@Serializable
private data class ExportData(
    val pano_version: Int,

    @JsonNames("edits")
    val simple_edits: List<SimpleEdit>?,
    @JsonNames("regex_edits")
    val regex_edits_legacy: List<RegexEditLegacy>? = null,
    val regex_rules: List<RegexEdit>?,
    val blocked_metadata: List<BlockedMetadata>?,
    val scrobble_sources: List<ScrobbleSource>?,
    val settings: MainPrefs.Public?,
)

enum class EditsMode {
    EDITS_NOPE,
    EDITS_REPLACE_ALL,
    EDITS_REPLACE_EXISTING,
    EDITS_KEEP_EXISTING,
}

@Serializable
private data class RegexEditLegacy(
    val order: Int = -1,
    val preset: String? = null,
    val name: String? = null,
    val pattern: String? = null,
    val replacement: String = "",

    val extractionPatterns: ExtractionPatternsLegacy? = null,

    val fields: Set<String>? = null,

    val packages: Set<String>? = null,
    val replaceAll: Boolean = false,
    val caseSensitive: Boolean = false,
    val continueMatching: Boolean = false,
)

@Serializable
private data class ExtractionPatternsLegacy(
    val extractionTrack: String,
    val extractionAlbum: String,
    val extractionArtist: String,
    val extractionAlbumArtist: String,
)

private object ExportDataSerializer :
    JsonTransformingSerializer<ExportData>(ExportData.serializer()) {
    override fun transformSerialize(element: JsonElement) = removeIdRecursively(element)

    private fun removeIdRecursively(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> buildJsonObject {
            element.forEach { (k, v) ->
                // remove database ID fields
                if (k != "_id") put(k, removeIdRecursively(v))
            }
        }

        is JsonArray ->
            buildJsonArray {
                element.forEach { add(removeIdRecursively(it)) }
            }

        else -> element
    }
}