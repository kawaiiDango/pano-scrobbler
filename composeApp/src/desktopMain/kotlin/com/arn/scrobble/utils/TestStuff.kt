package com.arn.scrobble.utils

import co.touchlab.kermit.Logger
import com.arn.scrobble.api.Requesters
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.URI


object TestStuff {
    fun test() {
        // test stuff
        val properties = System.getProperties()
        Logger.i("\n\nSystem properties:")
        properties.forEach { (key, value) -> Logger.i("$key: $value") }

        testProxy()
    }

    private fun testProxy() {
        val testUrls = arrayOf(
            "http://www.example.com",
            "https://www.example.com",
            "socket://www.example.com"
        )
        val proxySelector = ProxySelector.getDefault()

        for (urlString in testUrls) {
            val uri = URI(urlString)
            println("\nProxy configuration for: $urlString")

            proxySelector.select(uri).forEach { proxy ->
                val addr = proxy.address() as? InetSocketAddress
                println("" + proxy.type() + ": " + addr?.hostName + ":" + addr?.port)
            }
        }

        // print ip
        GlobalScope.launch {
            try {
                Requesters.baseKtorClient.get("https://myip.wtf/json").bodyAsText().let {
                    Logger.i("IP info:\n$it")
                }
            } catch (e: Exception) {
                Logger.e(e) { "Failed to get IP info" }
            }
        }
    }
}