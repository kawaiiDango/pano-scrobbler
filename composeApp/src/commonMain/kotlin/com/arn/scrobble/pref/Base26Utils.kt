package com.arn.scrobble.pref

object Base26Utils {
    private val alphabet = ('A'..'Z').joinToString("")
    const val PORT_START = 8500
    const val PORT_END = 9000

    fun encodeIpPort(ip: String, port: Int): String {
        val ipParts = ip.split(".").map { it.toInt() }
        val (ipPrefix, dropN) = when {
            ipParts[0] == 10 -> "A" to 1
            ipParts[0] == 172 -> "B" to 1
            ipParts[0] == 192 && ipParts[1] == 168 -> "C" to 2
            else -> throw IllegalArgumentException("Invalid code")
        }
        val ipSuffix = ipParts.drop(dropN)
        val portPart = port - 8500
        val restOfTheParts = ipSuffix + portPart
        return ipPrefix + restOfTheParts.joinToString("") { encode(it) }
    }

    fun decodeIpPort(encoded: String): Pair<String, Int> {
        val parts = encoded.drop(1).chunked(2).map { decode(it) }
        val ipPrefix = when (encoded.first()) {
            'A' -> "10"
            'B' -> "172"
            'C' -> "192.168"
            else -> throw IllegalArgumentException("Invalid code")
        }
        val ip = ipPrefix + "." + parts.dropLast(1).joinToString(".")
        val port = parts.last() + 8500
        return ip to port
    }

    private fun encode(num: Int): String {
        var n = num
        val result = StringBuilder()
        while (n > 0) {
            val digit = n % 26
            result.append(alphabet[digit])
            n /= 26
        }
        return result.reverse().toString().padStart(2, 'A')
    }

    private fun decode(encoded: String): Int {
        var num = 0
        for (char in encoded) {
            num = num * 26 + alphabet.indexOf(char)
        }
        return num
    }
}