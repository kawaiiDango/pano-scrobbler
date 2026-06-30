package com.arn.scrobble.api.license

import com.arn.scrobble.billing.Security
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import kotlin.io.encoding.Base64


private const val RS_PUBLIC_KEY_BASE64 =
    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnElQD+PNdex6IZ1nq58KDJPz40GBgOIbUs3GrbaPsONcEy8+AEhZmpPDcVB/e931pExsGPdRrjd2cplJ8pUXvxBG5knyJv7EPO3VUnppbipqYhaSe9bH4nK5kuNROB/J3mggVMxZmgoDe2QHacrNbnfjS96pFc58MAjQPPCn6TAXA1H3WajvNcRnplBYK7N0ap/YT1dbMato4fl/0iT1J57bDz+J+w/DcewOOg7YPWxVN+p9WZyLKwgQ8y/1QybEi9IYfIw3INqVS11vx5f+79ZkY+xGAM9JHm7T71dDZc4rJPibUnnQ+R5J2jFz564wdio6i1zpKwUpNQgYbfpkPQIDAQAB"

private const val ES_PUBLIC_KEY_BASE64 =
    "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEV+fOgN2AP5YCeBw8D8IBSkkzNDAgBc5u7qKGh3qOziWwKvl2HV3z2j2UmWdCOoQwR2XIYueIy4xoti1aUbv7IQ=="

@Serializable
data class JwtHeader(val alg: String, val typ: String)

@Serializable
data class JwtPayload(
    val sub: String,
    val aud: String,
    val nbf: Long,
    val exp: Long,
)

@Serializable
data class LicenseCheckResponse(
    val code: Int,
    val message: String,
)

@Serializable
private data class LicenseCheckRequest(
    val did: String,
    val token: String,
)

object LicenseChecker {
    suspend fun checkLicenseOnline(
        httpPost: suspend (url: String, body: String) -> String,
        url: String,
        did: String,
        token: String,
    ): Result<LicenseCheckResponse> {
        val lcr = LicenseCheckRequest(did, token)

        return runCatching {
            val jsonBodyString = Json.encodeToString(lcr)
            httpPost(url, jsonBodyString).let {
                Json.decodeFromString<LicenseCheckResponse>(it)
            }
        }
    }


    fun validateJwt(token: String, aud: String): Boolean {
        val b64 = Base64
            .UrlSafe
            .withPadding(Base64.PaddingOption.ABSENT)

        try {
            val parts = token.split(".")
            if (parts.size != 3) return false

            val header = b64.decode(parts[0]).decodeToString().let {
                Json.decodeFromString<JwtHeader>(it)
            }

            if (header.typ != "JWT")
                return false

            val payload = b64.decode(parts[1]).decodeToString().let {
                Json.decodeFromString<JwtPayload>(it)
            }

            if (!(payload.aud == aud && payload.nbf < System.currentTimeMillis() / 1000 && payload.exp > System.currentTimeMillis() / 1000))
                return false

            val signature = b64.decode(parts[2])

            val data = "${parts[0]}.${parts[1]}".toByteArray()

            return when (header.alg) {
                "RS256" -> verifyRsSignature(data, signature)
                "ES256" -> verifyEsSignature(data, signature)
                else -> false
            }
        } catch (e: Exception) {
            return false
        }
    }

    private fun verifyRsSignature(
        data: ByteArray,
        signature: ByteArray,
    ): Boolean {
        val publicKey = Security.loadPublicKey(RS_PUBLIC_KEY_BASE64)
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initVerify(publicKey)
        sig.update(data)
        return sig.verify(signature)
    }

    /**
     * Converts an IEEE P1363 ECDSA signature (raw R || S, as used in JWS / JWT)
     * into the DER/ASN.1 format required by java.security.Signature.
     *
     * For ES256 (P-256 curve), the P1363 signature is always 64 bytes:
     * 32 bytes for R followed by 32 bytes for S.
     */
    fun p1363ToDer(p1363Signature: ByteArray): ByteArray {
        require(p1363Signature.size == 64) {
            "ES256 P1363 signature must be 64 bytes (32-byte R + 32-byte S), got ${p1363Signature.size}"
        }

        val rBytes = p1363Signature.copyOfRange(0, 32)
        val sBytes = p1363Signature.copyOfRange(32, 64)

        val rEncoded = encodeDerInteger(rBytes)
        val sEncoded = encodeDerInteger(sBytes)

        val sequenceContent = ByteArrayOutputStream().apply {
            write(rEncoded)
            write(sEncoded)
        }.toByteArray()

        return ByteArrayOutputStream().apply {
            write(0x30) // SEQUENCE tag
            write(encodeDerLength(sequenceContent.size))
            write(sequenceContent)
        }.toByteArray()
    }

    /**
     * Encodes a raw unsigned big-endian integer (as found in a P1363 signature)
     * as a DER INTEGER (tag 0x02): strips unnecessary leading zero bytes, and
     * prepends a zero byte if the high bit is set so the value isn't
     * misinterpreted as negative under DER's two's-complement rule.
     */
    private fun encodeDerInteger(raw: ByteArray): ByteArray {
        var start = 0
        while (start < raw.size - 1 && raw[start] == 0.toByte()) {
            start++
        }
        val trimmed = raw.copyOfRange(start, raw.size)

        val needsPadding = trimmed[0].toInt() and 0x80 != 0
        val value = if (needsPadding) byteArrayOf(0x00) + trimmed else trimmed

        return ByteArrayOutputStream().apply {
            write(0x02) // INTEGER tag
            write(encodeDerLength(value.size))
            write(value)
        }.toByteArray()
    }

    /** Encodes a DER length (short form is sufficient here; R/S are at most 33 bytes). */
    private fun encodeDerLength(length: Int): ByteArray {
        require(length < 0x80) { "Unexpectedly large DER length for an ES256 R/S component: $length" }
        return byteArrayOf(length.toByte())
    }


    private fun verifyEsSignature(
        data: ByteArray,
        signature: ByteArray,
    ): Boolean {
        val derSignature = p1363ToDer(signature)
        val publicKeyBytes = Base64.decode(ES_PUBLIC_KEY_BASE64)
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val keyFactory = KeyFactory.getInstance("EC")
        val publicKey = keyFactory.generatePublic(keySpec)

        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initVerify(publicKey)
        sig.update(data)

        return sig.verify(derSignature)
    }

}