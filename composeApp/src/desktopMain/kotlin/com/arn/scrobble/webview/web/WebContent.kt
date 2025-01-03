package com.arn.scrobble.webview.web

/**
 * Created By Kevin Zou On 2023/9/5
 */
sealed class WebContent {
    data class Url(
        val url: String,
        val additionalHttpHeaders: Map<String, String> = emptyMap(),
    ) : WebContent()

    data class Data(
        val data: String,
        val baseUrl: String? = null,
        val encoding: String = "utf-8",
        val mimeType: String? = null,
        val historyUrl: String? = null,
    ) : WebContent()

    data class Post(
        val url: String,
        val postData: ByteArray,
    ) : WebContent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Post

            if (url != other.url) return false
            if (!postData.contentEquals(other.postData)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + postData.contentHashCode()
            return result
        }
    }

    @Deprecated("Use state.lastLoadedUrl instead")
    fun getCurrentUrl(): String? {
        return when (this) {
            is Url -> url
            is Data -> baseUrl
            is Post -> url
            is NavigatorOnly -> throw IllegalStateException("Unsupported")
        }
    }

    object NavigatorOnly : WebContent()
}

internal fun WebContent.withUrl(url: String) = when (this) {
    is WebContent.Url -> copy(url = url)
    else -> WebContent.Url(url)
}