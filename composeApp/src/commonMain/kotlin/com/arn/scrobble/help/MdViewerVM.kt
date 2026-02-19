package com.arn.scrobble.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getResult
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.VariantStuff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.network_error

class MdViewerVM(
    upstreamUrl: String,
    embeddedPath: String? = null
) : ViewModel() {
    private val _searchTerm = MutableStateFlow("")
    private var inited = false
    private val currentPlatform = when {
        PlatformStuff.isDesktop -> MdTag.desktop
        PlatformStuff.isTv -> MdTag.tv
        else -> MdTag.android
    }

    private val nonPlayTag = if (VariantStuff.billingRepository.needsActivationCode)
        MdTag.nonplay
    else
        null

    val mdItems = flow {
        var embeddedVersion: String? = null

        if (embeddedPath != null) {
            embeddedVersion = Res.readBytes(embeddedPath).decodeToString()
            emit(embeddedVersion)
        }

        Requesters.genericKtorClient
            .getResult<String>(upstreamUrl)
            .onSuccess {
                if (embeddedVersion != it)
                    emit(it)
            }.onFailure {
                // if we have an embedded version, we can ignore the error
                if (embeddedVersion == null) {
                    val directUrl = upstreamUrl.removeSuffix(".md")
                    emit(
                        "### " + getString(Res.string.network_error) + "\n\n" +
                                "View directly at: [$directUrl]($directUrl)\n\n"
                    )
                }
            }
    }
        .mapLatest { parseMd(it) }
        .map { items ->
            items.filter { it.tag == null || it.tag == currentPlatform || it.tag == nonPlayTag }
        }
        .combine(_searchTerm.debounce {
            if (!inited) {
                inited = true
                0
            } else {
                500L
            }
        }) { faqs, term ->
            if (term.isBlank()) faqs
            else faqs.filter {
                it.header.contains(term, ignoreCase = true) ||
                        it.content.contains(term, ignoreCase = true)
            }

        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun setFilter(searchTerm: String) {
        _searchTerm.value = searchTerm.trim()
    }

    private fun parseMd(md: String): List<MdItem> {
        val lines = md.lineSequence()
        val items = mutableListOf<MdItem>()
        var currentHeader: String? = null
        val currentContent = StringBuilder()

        fun flushEntry() {
            val header = currentHeader ?: return

            val content = currentContent.trim()

            // Extract platform tags like [android], [desktop]
            val (tag, cleanHeader) = MdTag.entries
                .firstOrNull { header.startsWith("[$it]", ignoreCase = true) }
                .let { tag ->
                    val cleanHeader = if (tag != null)
                        header.removePrefix("[$tag]").trim()
                    else
                        header.trim()

                    tag to cleanHeader
                }

            // Parse inline entities from the answer
            val entities = mutableListOf<MdEntity>()
            val plainBuilder = StringBuilder()

            var i = 0
            val src = content

            fun isEscaped(index: Int): Boolean {
                if (index == 0) return false
                var backslashCount = 0
                var j = index - 1
                while (j >= 0 && src[j] == '\\') {
                    backslashCount++
                    j--
                }
                return backslashCount % 2 == 1
            }

            fun parseInlineEntity(
                type: MdEntityType,
                openDelimiter: String,
                closeDelimiter: String
            ): Int? {
                if (i + openDelimiter.length > src.length) return null
                if (!src.startsWith(openDelimiter, i)) return null

                val closeIdx = src.indexOf(closeDelimiter, i + openDelimiter.length)
                if (closeIdx == -1) return null

                val text = src.substring(i + openDelimiter.length, closeIdx)
                val start = plainBuilder.length
                plainBuilder.append(text)
                val end = plainBuilder.length

                entities.add(MdEntity(type = type, start = start, end = end))

                return closeIdx + closeDelimiter.length
            }

            while (i < src.length) {
                // Handle escaped characters
                if (src[i] == '\\' && i + 1 < src.length) {
                    val nextChar = src[i + 1]
                    if (nextChar in setOf('*', '_', '[', ']', '(', ')', '`', '\\')) {
                        plainBuilder.append(nextChar)
                        i += 2
                        continue
                    }
                }

                // Markdown link: [text](url)
                if (src[i] == '[' && !isEscaped(i)) {
                    val closeBracket = src.indexOf(']', i + 1)
                    if (closeBracket != -1
                        && !isEscaped(closeBracket)
                        && closeBracket + 1 < src.length
                        && src[closeBracket + 1] == '('
                    ) {
                        val closeParen = src.indexOf(')', closeBracket + 2)
                        if (closeParen != -1 && !isEscaped(closeParen)) {
                            val linkText = src.substring(i + 1, closeBracket)
                            val url = src.substring(closeBracket + 2, closeParen).trim()

                            // Validate URL - reject if it contains unescaped spaces
                            if (url.isNotEmpty() && !url.contains(Regex("(?<!%)\\s"))) {
                                val start = plainBuilder.length
                                plainBuilder.append(linkText)
                                val end = plainBuilder.length

                                val entity = MdEntity(
                                    type = MdEntityType.link,
                                    start = start,
                                    end = end,
                                    url = url
                                )
                                entities.add(entity)

                                i = closeParen + 1
                                continue
                            }
                        }
                    }
                }

                // Bold+Italic: ***text***
                parseInlineEntity(MdEntityType.bold, "***", "***")
                    ?.let { newI ->
                        // Add italic entity for the same range
                        val lastEntity = entities.last()
                        entities.add(lastEntity.copy(type = MdEntityType.italic))
                        i = newI
                        continue
                    }

                // Bold: **text** or __text__
                parseInlineEntity(MdEntityType.bold, "**", "**")
                    ?.let { newI -> i = newI; continue }
                parseInlineEntity(MdEntityType.bold, "__", "__")
                    ?.let { newI -> i = newI; continue }

                // Italic: *text* or _text_
                parseInlineEntity(MdEntityType.italic, "*", "*")
                    ?.let { newI -> i = newI; continue }
                parseInlineEntity(MdEntityType.italic, "_", "_")
                    ?.let { newI -> i = newI; continue }

                // Inline code: `text`
                parseInlineEntity(MdEntityType.code, "`", "`")
                    ?.let { newI -> i = newI; continue }

                plainBuilder.append(src[i])
                i++
            }

            val entry = MdItem(
                header = cleanHeader,
                content = plainBuilder.toString(),
                entities = entities,
                tag = tag
            )

            items.add(entry)
            currentHeader = null
            currentContent.clear()
        }

        for (line in lines) {
            when {
                line.startsWith("### ") -> {
                    flushEntry()
                    currentHeader = line.removePrefix("### ").trim()
                }

                line.startsWith("#### ") -> {
                    flushEntry()
                    currentHeader = line.removePrefix("#### ").trim()
                }
                // H1 title line or blank before first question — skip
                line.startsWith("# ") -> { /* skip title */
                }
                // Accumulate answer lines
                currentHeader != null -> {
                    val trimmedLine = line.trim()
                    // Handle list items (- or *)
                    if (trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ")) {
                        // Start new list item
                        if (currentContent.isNotEmpty() && !currentContent.endsWith("\n\n")) {
                            currentContent.append("\n\n")
                        }
                        currentContent.append("• ").append(trimmedLine.substring(2))
                    } else if (trimmedLine.isNotEmpty() && currentContent.endsWith("• ")) {
                        // Continuation of previous list item (line without marker)
                        currentContent.append(" ").append(trimmedLine)
                    } else if (trimmedLine.isNotEmpty()) {
                        // Regular paragraph text
                        if (currentContent.isNotEmpty() &&
                            !currentContent.endsWith(" ") &&
                            !currentContent.endsWith("\n\n")
                        ) {
                            currentContent.append(" ")
                        }
                        currentContent.append(trimmedLine)
                    } else if (trimmedLine.isEmpty() && currentContent.isNotEmpty()) {
                        // Blank line - end current paragraph/list item
                        if (!currentContent.endsWith("\n\n")) {
                            currentContent.append("\n\n")
                        }
                    }
                }
            }
        }
        flushEntry()

        return items
    }
}