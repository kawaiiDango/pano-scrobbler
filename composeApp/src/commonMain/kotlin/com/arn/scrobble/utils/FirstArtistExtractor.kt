package com.arn.scrobble.utils

import net.openhft.hashing.ByteBufferAccess
import net.openhft.hashing.LongHashFunction
import pano_scrobbler.composeapp.generated.resources.Res
import java.nio.ByteBuffer

object FirstArtistExtractor {
    private lateinit var knownArtists: Set<Long>
    private var initialized = false
    private val xxhash by lazy { LongHashFunction.xx3() }

    private val COMMON_DELIMITERS = arrayOf(
        ", ",
        "、",
        "، ",
        " و ",
        "፣ ",
        ";",
        " & ",
        " / ",
    )

    // the rest is for youtube music
    private val ANDS = arrayOf(
        " and ",
        " en ",
        " və ",
        " dan ",
        " i ",
        " a ",
        " og ",
        " und ",
        " ja ",
        " y ",
        " eta ",
        ", at ",
        " et ",
        " e ",
        ", ne-",
        " na ",
        " un ",
        " ir ",
        " és ",
        " va ",
        " dhe ",
        " și ",
        " in ",
        " och ",
        " và ",
        " ve ",
        " и ",
        " жана ",
        " και ",
        " և ",
        " ו-",
        " اور ",
        "، و ",
        " र ",
        " आणि ",
        " और ",
        " আৰু ",
        " এবং ",
        " ਅਤੇ ",
        " અને ",
        ", ଓ ",
        " மற்றும் ",
        " మరియు ",
        ", ಮತ್ತು ",
        " എന്നിവ",
        ", සහ ",
        " และ",
        " ແລະ ",
        "နှင့် ",
        " და ",
        " እና ",
        " និង ",
        "和",
        "及",
        " 및 ",
    )

    private fun initFromBytes(byteArray: ByteArray) {
        val knownArtists = HashSet<Long>()

        // read the byte array as a sequence of 64-bits (longs)
        for (i in byteArray.indices step 8) {
            if (i + 8 <= byteArray.size) {
                val longValue = byteArray
                    .copyOfRange(i, i + 8)
                    .fold(0L) { acc, byte ->
                        (acc shl 8) or (byte.toLong() and 0xFF)
                    }
                knownArtists.add(longValue)
            }
        }

        this.knownArtists = knownArtists
    }


    suspend fun extract(artistString: String, useAnd: Boolean): String {
        if (!initialized) {
            val bytes = Res.readBytes("files/musicbrainz_artist_hashes.bin")
            initFromBytes(bytes)
            initialized = true
        }

        val trimmed = artistString.trim()
        if (trimmed.isEmpty()) return trimmed

        if (!PlatformStuff.isJava8OrGreater) return trimmed

        var byteBuffer: ByteBuffer? = null

        fun String.x(): Long {
            //  the first call to this function is always the with longest string. Will allocate max space.
            val byteArray = encodeToByteArray(throwOnInvalidSequence = false)

            if (byteBuffer == null) {
                byteBuffer = ByteBuffer.allocate(byteArray.size)
            }

            byteBuffer.clear()
            byteBuffer.put(byteArray)
            byteBuffer.flip()

            return xxhash.hash(
                byteBuffer,
                // force this instance to avoid UnsafeAccess and native crashes on 32-bit Samsungs
                ByteBufferAccess.INSTANCE,
                byteBuffer.position().toLong(),
                byteBuffer.remaining().toLong()
            )
        }

        // Quick check: if entire string is known
        if (knownArtists.contains(trimmed.x())) {
            return trimmed
        }

        val delimiters = if (useAnd) {
            COMMON_DELIMITERS + ANDS
        } else {
            COMMON_DELIMITERS
        }

        // Find the earliest delimiter position
        var earliestDelimiterPos = Int.MAX_VALUE
        for (delimiter in delimiters) {
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

            if (knownArtists.contains(candidate.x())) {
                // Verify this ends appropriately
                val nextPos = i + 1
                if (nextPos >= trimmed.length ||
                    delimiters.any { delimiter ->
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