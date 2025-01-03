package com.arn.scrobble.logger

import co.touchlab.kermit.Logger
import java.io.OutputStream


// Custom OutputStream to redirect stderr to Logger
class StderrOutputStream : OutputStream() {

    private val buffer = StringBuilder()

    override fun write(b: Int) {
        if (b == '\n'.code) {
            // don't use Logger.e here, it will cause an infinite recursion
            Logger.w { buffer.toString() }
            buffer.clear()
        } else {
            buffer.append(b.toChar())
        }
    }
}
