package com.arn.scrobble.utils

import co.touchlab.kermit.Logger
import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


object TestStuff {
    fun test() {
        // test stuff
        val properties = System.getProperties()
        Logger.i("\n\nSystem properties:")
        properties.forEach { (key, value) -> Logger.i("$key: $value") }

        testCerts()
    }

    private fun testCerts() {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)

        val trustManager = tmf.trustManagers[0] as X509TrustManager
        Logger.i("Trust store type: ${System.getProperty("javax.net.ssl.trustStoreType")}")
        Logger.i("Trusted CA count: ${trustManager.acceptedIssuers.size}")
        trustManager.acceptedIssuers.take(5).forEach {
            Logger.i("  CA: ${it.subjectX500Principal.name}")
        }
    }
}