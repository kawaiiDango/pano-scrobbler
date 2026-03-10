package com.arn.scrobble.pref

import java.io.InputStream


class LimitedInputStream(private val source: InputStream, limit: Int) : InputStream() {
    private var remaining = limit

    override fun read(): Int {
        if (remaining <= 0) return -1
        return source.read().also { if (it != -1) remaining-- }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (remaining <= 0) return -1
        return source.read(b, off, minOf(len, remaining))
            .also { if (it != -1) remaining -= it }
    }
}