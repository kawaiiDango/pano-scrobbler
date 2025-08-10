package com.arn.scrobble.utils

import pano_scrobbler.composeapp.generated.resources.Res
import java.io.InputStream

object FirstArtistExtractor {
    private lateinit var knownArtists: Set<String>
    private var initialized = false


    private val DELIMITERS = arrayOf(", ", "、 ", ";", " & ", " / ")

    private fun initFromInputStream(inputStream: InputStream) {
        val knownArtists = HashSet<String>()

        // this does not include the trailing newline in the file
        inputStream.bufferedReader().useLines { lines ->
            knownArtists.addAll(lines)
        }

        this.knownArtists = knownArtists
    }

    suspend fun extract(artistString: String): String {
        if (!initialized) {
            val bytes = Res.readBytes("files/musicbrainz_artists_with_delimiters.txt")
            initFromInputStream(bytes.inputStream())
            initialized = true
        }

        val trimmed = artistString.trim()
        if (trimmed.isEmpty()) return trimmed

        // Quick check: if entire string is known
        if (knownArtists.contains(trimmed)) {
            return trimmed
        }

        // Find the earliest delimiter position
        var earliestDelimiterPos = Int.MAX_VALUE
        for (delimiter in DELIMITERS) {
            val pos = trimmed.indexOf(delimiter)
            if (pos != -1 && pos < earliestDelimiterPos) {
                earliestDelimiterPos = pos
            }
        }

        // If no delimiters found, return entire string
        if (earliestDelimiterPos == Int.MAX_VALUE) {
            return trimmed
        }

        // Check all possible prefixes up to the first delimiter + some buffer
        // (in case the artist name extends beyond the first delimiter)
        val searchLimit = minOf(trimmed.length, earliestDelimiterPos + 20) // reasonable buffer

        var longestMatch = ""

        // Use StringBuilder to avoid creating multiple substring objects
        val sb = StringBuilder()
        for (i in 0 until searchLimit) {
            sb.append(trimmed[i])
            val candidate = sb.toString()

            if (knownArtists.contains(candidate)) {
                // Verify this ends appropriately
                val nextPos = i + 1
                if (nextPos >= trimmed.length ||
                    DELIMITERS.any { delimiter ->
                        trimmed.substring(nextPos).startsWith(delimiter)
                    }
                ) {
                    longestMatch = candidate
                }
            }
        }

        if (longestMatch.isNotEmpty()) {
            return longestMatch
        }

        // Fallback: return text up to first delimiter
        return trimmed.substring(0, earliestDelimiterPos).trim()
    }
}