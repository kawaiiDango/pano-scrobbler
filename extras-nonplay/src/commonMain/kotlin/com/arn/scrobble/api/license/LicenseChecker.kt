package com.arn.scrobble.api.license

import com.arn.scrobble.billing.Security
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.PublicKey
import java.security.Signature
import kotlin.io.encoding.Base64

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
    private const val JWT_SIGNATURE_ALGORITHM = "SHA256withRSA"

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


    fun validateJwt(token: String, aud: String, base64PublicKey: String): Boolean {
        val b64 = Base64
            .UrlSafe
            .withPadding(Base64.PaddingOption.ABSENT)

        try {
            val parts = token.split(".")
            if (parts.size != 3) return false

            val header = b64.decode(parts[0]).decodeToString().let {
                Json.decodeFromString<JwtHeader>(it)
            }

            if (!(header.alg == "RS256" && header.typ == "JWT"))
                return false

            val payload = b64.decode(parts[1]).decodeToString().let {
                Json.decodeFromString<JwtPayload>(it)
            }

            if (!(payload.aud == aud && payload.nbf < System.currentTimeMillis() / 1000 && payload.exp > System.currentTimeMillis() / 1000))
                return false

            val signature = b64.decode(parts[2])

            val data = "${parts[0]}.${parts[1]}".toByteArray()

            val publicKey = Security.loadPublicKey(base64PublicKey)
            verifySignature(data, signature, publicKey)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun verifySignature(
        data: ByteArray,
        signature: ByteArray,
        publicKey: PublicKey,
    ): Boolean {
        val sig = Signature.getInstance(JWT_SIGNATURE_ALGORITHM)
        sig.initVerify(publicKey)
        sig.update(data)
        return sig.verify(signature)
    }

}