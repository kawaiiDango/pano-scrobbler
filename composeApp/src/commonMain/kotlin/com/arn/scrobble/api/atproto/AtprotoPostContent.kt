package com.arn.scrobble.api.atproto

import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteReadChannel

// Ktor's OkHttp engine treats channel-backed bodies as one-shot.
class AtprotoPostContent(text: String, override val contentType: ContentType) :
    OutgoingContent.ReadChannelContent() {
    private val bytes = text.encodeToByteArray()
    private val channel = ByteReadChannel(bytes)
    override val contentLength = bytes.size.toLong()

    override fun readFrom() = channel
}
