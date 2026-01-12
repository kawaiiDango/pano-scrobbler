package com.arn.scrobble.pref

/**
 * Encodes/decodes an IPv4 address + port (8500..8755 inclusive) into an 8-character Crockford Base32 code.
 *
 * Packing (40 bits total => 8 chars):
 *   ip32  = a<<24 | b<<16 | c<<8 | d
 *   off8  = port - PORT_BASE   (0..255)
 *   value = (ip32 << 8) | off8
 *
 * Code:
 *   - Crockford Base32 alphabet (no I, L, O, U)
 *   - Encodes to exactly 8 chars (left-padded with '0')
 *   - Decoder is tolerant: ignores '-' and whitespace, and maps O->0, I/L->1.
 */

object IpPortCode {
    const val PORT_BASE = 8500
    const val PORT_MAX = PORT_BASE + 255
    const val CODE_LENGTH = 8

    // Crockford Base32 alphabet (excludes I, L, O, U)
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    // Decode map for Crockford Base32 (case-insensitive with aliases)
    private val DECODE_MAP = buildMap<Char, Int> {
        ALPHABET.forEachIndexed { index, char ->
            put(char, index)
            put(char.lowercaseChar(), index)
        }
        // Crockford aliases:  I/L → 1, O → 0
        put('I', 1); put('i', 1)
        put('L', 1); put('l', 1)
        put('O', 0); put('o', 0)
    }

    /**
     * Encodes an IPv4 address and port into an 8-character Crockford Base32 code.
     *
     * @param ip IPv4 address as a string (e.g., "192.168.1.1")
     * @param port Port number (must be in range 8500..8755)
     * @return 8-character Base32 code
     */
    fun encode(ip: String, port: Int): String {
        require(port in PORT_BASE..PORT_MAX) {
            "Port must be in range $PORT_BASE..$PORT_MAX"
        }

        // Parse IPv4 address
        val octets = ip.split(".")
        require(octets.size == 4) { "Invalid IPv4 address format" }

        val bytes = octets.map { octet ->
            val value = octet.toIntOrNull()
            require(value != null && value in 0..255) {
                "Invalid IPv4 octet: $octet"
            }
            value
        }

        // Pack into 40 bits:  ip32 = a<<24 | b<<16 | c<<8 | d
        val ip32 = (bytes[0].toLong() shl 24) or
                (bytes[1].toLong() shl 16) or
                (bytes[2].toLong() shl 8) or
                bytes[3].toLong()

        // Offset = port - PORT_BASE (0..255)
        val offset = port - PORT_BASE

        // value = (ip32 << 8) | offset
        val value = (ip32 shl 8) or offset.toLong()

        // Encode to Base32: 40 bits = 8 chars × 5 bits each
        return buildString(8) {
            repeat(8) { i ->
                val index = (value shr (35 - i * 5)) and 0x1F
                append(ALPHABET[index.toInt()])
            }
        }
    }

    /**
     * Decodes an 8-character Crockford Base32 code back to IPv4 and port.
     *
     * @param code 8-character Base32 code
     * @return Pair of (IPv4 address string, port number)
     */
    fun decode(code: String): Pair<String, Int> {
        val cleaned = buildString(code.length) {
            for (ch in code) if (!ch.isWhitespace() && ch != '-') append(ch)
        }

        require(cleaned.length == CODE_LENGTH) { "Code must be exactly $CODE_LENGTH characters" }

        // Decode Base32 to 40-bit value
        var value = 0L
        for (char in cleaned) {
            val digit = DECODE_MAP[char]
                ?: throw IllegalArgumentException("Invalid Base32 character: $char")
            value = (value shl 5) or digit.toLong()
        }

        // Unpack:  offset = lower 8 bits, ip32 = upper 32 bits
        val offset = (value and 0xFF).toInt()
        val ip32 = (value shr 8).toInt()

        val port = PORT_BASE + offset
        val a = (ip32 ushr 24) and 0xFF
        val b = (ip32 ushr 16) and 0xFF
        val c = (ip32 ushr 8) and 0xFF
        val d = ip32 and 0xFF

        return "$a.$b.$c.$d" to port
    }
}