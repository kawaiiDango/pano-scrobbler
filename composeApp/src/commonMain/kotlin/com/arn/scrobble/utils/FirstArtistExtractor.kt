package com.arn.scrobble.utils

import com.appmattus.crypto.Algorithm
import com.arn.scrobble.db.ArtistWithDelimiters
import pano_scrobbler.composeapp.generated.resources.Res
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(ExperimentalUnsignedTypes::class)
object FirstArtistExtractor {
    private var sortedHashes: ULongArray? = null
    private var initialized = false
    private val xxh3 by lazy { Algorithm.XXH3_64() }

    private val COMMON_DELIMITERS = arrayOf(
        ", ",
        "、",
        "، ",
        " و ",
        "፣ ",
        ";",
        " & ",
        "＆",
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

    fun initFromAssetAndUserList(
        assetBytes: ByteArray,
        userList: List<ArtistWithDelimiters>,
    ) {
        val buffer = ByteBuffer.wrap(assetBytes).order(ByteOrder.BIG_ENDIAN)
        val assetHashes = ULongArray(assetBytes.size / Long.SIZE_BYTES) {
            buffer.getLong().toULong()
        }

        val userStrings = userList
            .map { it.artist.trim().lowercase() }
            .filter { it.isNotEmpty() }

        if (userStrings.isEmpty()) {
            sortedHashes = assetHashes
            return
        }

        // Hash and sort the user strings
        val digest = xxh3.createDigest()

        val userHashes = ULongArray(userStrings.size) { i ->
            val digestOutputBytes = ByteArray(digest.digestLength)

            val byteArray = userStrings[i].encodeToByteArray(throwOnInvalidSequence = false)

            digest.reset()
            digest.update(byteArray)

            digest.digest(digestOutputBytes, 0, digestOutputBytes.size)

            // convert digestOutputBytes to long
            digestOutputBytes.fold(0L) { acc, byte ->
                (acc shl 8) or (byte.toLong() and 0xFF)
            }.toULong()
        }

        userHashes.sort()

        // Merge the two sorted arrays using the two-pointer technique
        val merged = ULongArray(assetHashes.size + userHashes.size)
        var i = 0 // pointer for asset hashes
        var j = 0 // pointer for user hashes
        var k = 0 // pointer for merged array

        while (i < assetHashes.size && j < userHashes.size) {
            if (assetHashes[i] < userHashes[j]) {
                merged[k++] = assetHashes[i++]
            } else {
                merged[k++] = userHashes[j++]
            }
        }

        while (i < assetHashes.size) {
            merged[k++] = assetHashes[i++]
        }

        while (j < userHashes.size) {
            merged[k++] = userHashes[j++]
        }

        sortedHashes = merged
    }

    fun contains(hash: Long) = sortedHashes!!.binarySearch(hash.toULong()) >= 0

    suspend fun extract(
        artistString: String,
        useAnd: Boolean,
        updatedUserAllowlist: List<ArtistWithDelimiters>?,
    ): String {
        if (!initialized || updatedUserAllowlist != null) {
            val bytes = Res.readBytes("files/musicbrainz_artist_hashes.bin")
            initFromAssetAndUserList(bytes, updatedUserAllowlist ?: emptyList())
            initialized = true
        }

        val trimmed = artistString.trim()
        val digest = xxh3.createDigest()
        val digestOutputBytes = ByteArray(digest.digestLength)

        if (trimmed.isEmpty()) return trimmed

        fun String.x(): Long {
            val byteArray = lowercase().encodeToByteArray(throwOnInvalidSequence = false)

            digest.reset()
            digest.update(byteArray)

            digest.digest(digestOutputBytes, 0, digestOutputBytes.size)

            // convert digestOutputBytes to long
            return digestOutputBytes.fold(0L) { acc, byte ->
                (acc shl 8) or (byte.toLong() and 0xFF)
            }
        }

        // Quick check: if entire string is known
        if (contains(trimmed.x())) {
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
        val searchLimit = trimmed.length.coerceAtMost(1000)

        var longestMatch = ""

        // Use StringBuilder to avoid creating multiple substring objects
        val sb = StringBuilder()
        for (i in 0 until searchLimit) {
            sb.append(trimmed[i])
            val candidate = sb.toString()

            if (contains(candidate.x())) {
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
        return trimmed.take(earliestDelimiterPos).trim()
    }
}