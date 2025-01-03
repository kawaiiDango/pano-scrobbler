package com.arn.scrobble.webview.web

import com.arn.scrobble.utils.Stuff
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory


class CustomURLStreamHandler : URLStreamHandler() {
    override fun openConnection(url: URL): URLConnection {
        return object : URLConnection(url) {
            override fun connect() {
//                println("Custom protocol URL: $url")
                // Do nothing else
            }
        }
    }
}

class CustomURLStreamHandlerFactory : URLStreamHandlerFactory {
    override fun createURLStreamHandler(protocol: String): URLStreamHandler? {
        if (Stuff.DEEPLINK_PROTOCOL_NAME == protocol) {
            return CustomURLStreamHandler()
        }
        return null // Return null for other protocols to use the default handler
    }
}