package com.arn.scrobble.pref

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.db.RegexEdit


object JsonHelpers {

    fun SimpleEdit.writeJson(writer: JsonWriter) {
        val edit = this
        writer.apply {
            beginObject()
            if (edit.legacyHash != null)
                name("hash").value(edit.legacyHash)
            else {
                name("origTrack").value(edit.origTrack)
                name("origAlbum").value(edit.origAlbum)
                name("origArtist").value(edit.origArtist)
            }
            name("track").value(edit.track)
            name("album").value(edit.album)
            name("albumArtist").value(edit.albumArtist)
            name("artist").value(edit.artist)
            endObject()
        }
    }

    fun SimpleEdit.readJson(reader: JsonReader): SimpleEdit {
        val edit = this
        reader.apply {
            beginObject()
            while (hasNext()) {
                val editName = nextName()
                if (reader.peek() == JsonToken.NULL) {
                    skipValue()
                    continue
                }
                val editVal = nextString()
                when (editName) {
                    "hash" -> edit.legacyHash = editVal
                    "track" -> edit.track = editVal
                    "album" -> edit.album = editVal
                    "albumArtist" -> edit.albumArtist = editVal
                    "artist" -> edit.artist = editVal
                    "origTrack" -> edit.origTrack = editVal
                    "origAlbum" -> edit.origAlbum = editVal
                    "origArtist" -> edit.origArtist = editVal
                }
            }
            if (edit.legacyHash != null) {
                edit.origTrack = edit.legacyHash!!
                edit.origAlbum = edit.legacyHash!!
                edit.origArtist = edit.legacyHash!!
            }
            endObject()
        }
        return this
    }

    fun RegexEdit.writeJson(writer: JsonWriter) {
        val edit = this
        writer.apply {
            beginObject()
            name("order").value(edit.order)
            edit.preset?.let { name("preset").value(it) }
            edit.name?.let { name("name").value(it) }
            edit.pattern?.let { name("pattern").value(it) }
            name("replacement").value(edit.replacement)
            edit.field?.let { name("field").value(it) }

            name("replaceAll").value(edit.replaceAll)
            name("caseSensitive").value(edit.caseSensitive)
            name("continueMatching").value(edit.continueMatching)
            endObject()
        }
    }

    fun RegexEdit.readJson(reader: JsonReader): RegexEdit {
        val edit = this
        reader.apply {
            beginObject()
            while (hasNext()) {
                val name = nextName()
                if (reader.peek() == JsonToken.NULL)
                    skipValue()
                else {
                    when (name) {
                        "order" -> edit.order = nextInt()
                        "preset" -> edit.preset = nextString()
                        "name" -> edit.name = nextString()
                        "pattern" -> edit.pattern = nextString()
                        "replacement" -> edit.replacement = nextString()
                        "field" -> edit.field = nextString()
                        "replaceAll" -> edit.replaceAll = nextBoolean()
                        "caseSensitive" -> edit.caseSensitive = nextBoolean()
                        "continueMatching" -> edit.continueMatching = nextBoolean()
                    }
                }
            }
            endObject()
        }
        return this
    }

    fun BlockedMetadata.writeJson(writer: JsonWriter) {
        val blockedMetadata = this
        writer.apply {
            beginObject()
            name("track").value(blockedMetadata.track)
            name("album").value(blockedMetadata.album)
            name("albumArtist").value(blockedMetadata.albumArtist)
            name("artist").value(blockedMetadata.artist)
            endObject()
        }
    }

    fun BlockedMetadata.readJson(reader: JsonReader): BlockedMetadata {
        val blockedMetadata = this
        reader.apply {
            beginObject()
            while (hasNext()) {
                val name = nextName()
                if (reader.peek() == JsonToken.NULL)
                    skipValue()
                else {
                    when (name) {
                        "track" -> blockedMetadata.track = nextString()
                        "album" -> blockedMetadata.album = nextString()
                        "albumArtist" -> blockedMetadata.albumArtist = nextString()
                        "artist" -> blockedMetadata.artist = nextString()
                    }
                }
            }
            endObject()
        }
        return this
    }
}