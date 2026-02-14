package com.arn.scrobble.logger

import java.io.OutputStream
import java.util.logging.Level
import java.util.logging.Logger


// Custom OutputStream to redirect stderr to Logger

class LoggerOutputStream(
    private val logger: Logger,
    private val level: Level
) : OutputStream() {

    private val buffer = StringBuilder()

    override fun write(b: Int) {
        // Accumulate message for logging
        if (b.toChar() == '\n') {
            flushBuffer()
        } else {
            buffer.append(b.toChar())
        }
    }

    override fun flush() {
        flushBuffer()
    }

    private fun flushBuffer() {
        if (buffer.isNotEmpty()) {
            logger.log(level, buffer.toString())
            buffer.setLength(0)
        }
    }
}