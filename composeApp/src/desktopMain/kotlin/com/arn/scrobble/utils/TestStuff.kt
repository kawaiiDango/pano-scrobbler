package com.arn.scrobble.utils

import co.touchlab.kermit.Logger
import java.nio.charset.Charset

object TestStuff {
    fun test() {
        // test stuff
        val properties = System.getProperties()
        Logger.i("\n\nSystem properties:")
        properties.forEach { (key, value) -> Logger.i("$key: $value") }

        // supported charsets

        val charsets = Charset.availableCharsets()
        Logger.i("\n\nAvailable charsets:")
        charsets.forEach { (name, charset) ->
            Logger.i("$name: ${charset.displayName()}")
        }
    }
}