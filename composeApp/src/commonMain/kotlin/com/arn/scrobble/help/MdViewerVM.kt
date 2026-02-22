package com.arn.scrobble.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getResult
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.VariantStuff
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
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

    private enum class MdTag {
        android, desktop, tv, nonplay
    }

    private val currentPlatform = when {
        PlatformStuff.isDesktop -> MdTag.desktop
        PlatformStuff.isTv -> MdTag.tv
        else -> MdTag.android
    }

    private val nonPlayTag = if (VariantStuff.billingRepository.needsActivationCode)
        MdTag.nonplay
    else
        null

    val mdBlocks = flow {
        val embeddedVersion = embeddedPath
            ?.let { Res.readBytes(embeddedPath).decodeToString() }
            ?.also { emit(it) }

        Requesters.genericKtorClient
            .getResult<String>(upstreamUrl)
            .onSuccess {
                if (embeddedVersion != it && !BuildKonfig.DEBUG || embeddedVersion == null)
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
        .mapLatest {
            MdParser.parseMarkdown(it)
        }
        .combine(
            _searchTerm.debounce {
                if (!inited) {
                    inited = true
                    0L
                } else {
                    500L
                }
            }
        ) { items, term ->
            val filtered = mutableListOf<MdNode.Block>()

            var keeping = true
            items.forEachIndexed { index, block ->
                if (block is MdNode.Block.Heading) {
                    keeping = if (index == 0 && block.level == 1) {
                        false
                    } else {
                        block.children.firstOrNull()
                            .let {
                                if (it is MdNode.InlineNode.TextSpan) {
                                    val tagStartIdx = it.text.indexOf("[")
                                    val tagEndIdx = it.text.indexOf("]")

                                    val tags =
                                        if (tagStartIdx == 0 && tagEndIdx > 0)
                                            it.text.substring(1, tagEndIdx).split(",")
                                                .mapNotNull { tag ->
                                                    val tag = tag.trim()
                                                    MdTag.entries.firstOrNull { mdTag ->
                                                        mdTag.name == tag
                                                    }
                                                }
                                        else
                                            emptyList()

                                    val platformFilter = tags.isEmpty() ||
                                            tags.contains(currentPlatform) ||
                                            tags.contains(nonPlayTag)

                                    val searchFilter =
                                        term.isBlank() || it.text.indexOf(
                                            term,
                                            if (tags.isNotEmpty()) tagEndIdx + 1 else 0,
                                            ignoreCase = true
                                        ) >= 0

                                    platformFilter && searchFilter
                                } else
                                    true
                            }
                    }
                }

                if (keeping) filtered.add(block)
            }

            filtered.toList()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun setFilter(searchTerm: String) {
        _searchTerm.value = searchTerm.trim()
    }
}