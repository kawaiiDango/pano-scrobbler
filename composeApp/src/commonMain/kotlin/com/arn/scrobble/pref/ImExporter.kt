package com.arn.scrobble.pref

import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.db.ArtistWithDelimiters
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
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
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

    private var exportData: ExportData? = null

    suspend fun export(writer: OutputStream): Boolean {
        val exportData = ExportData(
            pano_version = BuildKonfig.VER_CODE,
            simple_edits = db.getSimpleEditsDao().allFlow().first().asReversed(),
            blocked_metadata = db.getBlockedMetadataDao().allFlow().first().asReversed(),
            regex_rules = db.getRegexEditsDao().allFlow().first(),
            artists_with_delimiters = db.getArtistsWithDelimitersDao().allFlow().first()
                .map { it.artist },
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
            artists_with_delimiters = null,
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

    fun createImportTypes(inputStream: InputStream): Set<ImportTypes> {
        val exportData = json.decodeFromStream<ExportData>(inputStream)

        val options = mutableSetOf<ImportTypes>()
        if (!exportData.simple_edits.isNullOrEmpty()) options += ImportTypes.simple_edits
        if (!exportData.regex_rules.isNullOrEmpty() || !exportData.regex_edits_legacy.isNullOrEmpty()) options += ImportTypes.regex_rules
        if (!exportData.blocked_metadata.isNullOrEmpty()) options += ImportTypes.blocked_metadata
        if (!exportData.artists_with_delimiters.isNullOrEmpty()) options += ImportTypes.artists_with_delimiters
        if (!exportData.scrobble_sources.isNullOrEmpty()) options += ImportTypes.scrobble_sources
        if (exportData.settings != null) options += ImportTypes.settings

        this.exportData = exportData
        return options
    }

    suspend fun import(
        importTypes: Set<ImportTypes>,
        writeMode: WriteMode
    ): Boolean {
        val exportData = this.exportData ?: return false

        if (ImportTypes.settings in importTypes && exportData.settings != null) {
            val allowedPackagesFiltered =
                exportData.settings.allowedPackages.filter(PlatformStuff::doesAppExist).toSet()

            val extractFirstArtistPackagesFiltered =
                exportData.settings.extractFirstArtistPackages.filter(PlatformStuff::doesAppExist)
                    .toSet()

            PlatformStuff.mainPrefs.updateData {
                it.updateFromPublicPrefs(
                    exportData.settings.copy(
                        allowedPackages = if (allowedPackagesFiltered.isEmpty() && exportData.settings.allowedPackages.isNotEmpty())
                            it.allowedPackages
                        else
                            allowedPackagesFiltered,
                        extractFirstArtistPackages = if (extractFirstArtistPackagesFiltered.isEmpty() && exportData.settings.extractFirstArtistPackages.isNotEmpty())
                            it.extractFirstArtistPackages
                        else
                            extractFirstArtistPackagesFiltered,
                    )
                )
            }
        }

        if (writeMode == WriteMode.replace_all) {
            if (ImportTypes.simple_edits in importTypes)
                db.getSimpleEditsDao().nuke()
            if (ImportTypes.regex_rules in importTypes)
                db.getRegexEditsDao().nuke()
            if (ImportTypes.blocked_metadata in importTypes)
                db.getBlockedMetadataDao().nuke()
            if (ImportTypes.artists_with_delimiters in importTypes)
                db.getArtistsWithDelimitersDao().nuke()
        }

        var migratedRegexEdits: List<RegexEdit>?

        if (ImportTypes.regex_rules in importTypes && exportData.regex_edits_legacy != null) {
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
                        continueMatching = it.continueMatching,
                    )

                }
            }
        } else
            migratedRegexEdits = null

        val regexEdits = migratedRegexEdits ?: exportData.regex_rules

        when (writeMode) {
            WriteMode.replace_all, WriteMode.replace_existing -> {
                if (ImportTypes.simple_edits in importTypes)
                    exportData.simple_edits?.let { db.getSimpleEditsDao().insert(it) }
                if (ImportTypes.blocked_metadata in importTypes)
                    exportData.blocked_metadata?.let { db.getBlockedMetadataDao().insert(it) }
            }

            WriteMode.keep_existing -> {
                if (ImportTypes.simple_edits in importTypes)
                    exportData.simple_edits?.let { db.getSimpleEditsDao().insertIgnore(it) }
                if (ImportTypes.blocked_metadata in importTypes)
                    exportData.blocked_metadata?.let { db.getBlockedMetadataDao().insertIgnore(it) }
            }
        }

        if (ImportTypes.regex_rules in importTypes)
            regexEdits?.let { db.getRegexEditsDao().import(it) }

        if (ImportTypes.artists_with_delimiters in importTypes)
            exportData.artists_with_delimiters
                ?.map { ArtistWithDelimiters(artist = it) }
                ?.let { db.getArtistsWithDelimitersDao().insert(it) }

        if (ImportTypes.scrobble_sources in importTypes)
            exportData.scrobble_sources?.let { db.getScrobbleSourcesDao().insert(it) }

        return true
    }

    enum class ImportTypes {
        settings,
        simple_edits,
        regex_rules,
        blocked_metadata,
        artists_with_delimiters,
        scrobble_sources,
    }

    enum class WriteMode {
        replace_all,
        replace_existing,
        keep_existing,
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
    val artists_with_delimiters: List<String>?,
    val scrobble_sources: List<ScrobbleSource>?,
    val settings: MainPrefs.Public?,
)


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