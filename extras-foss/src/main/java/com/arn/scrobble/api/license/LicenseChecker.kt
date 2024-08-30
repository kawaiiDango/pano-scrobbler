package com.arn.scrobble.api.license

import android.os.Build
import com.arn.scrobble.billing.Security
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.decodeBase64String
import io.ktor.util.encodeBase64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature

@Serializable
data class JwtHeader(val alg: String, val typ: String)

@Serializable
data class JwtPayload(
    val sub: String,
    val aud: String,
    val nbf: Long,
    val exp: Long
)

@Serializable
data class LicenseCheckResponse(
    val code: Int,
    val message: String
)

@Serializable
private data class LicenseCheckRequest(
    val did: String,
    val token: String
)

object LicenseChecker {
    private const val JWT_SIGNATURE_ALGORITHM = "SHA256withRSA"

    suspend fun checkLicenseOnline(
        client: HttpClient,
        url: String,
        token: String,
    ): Result<LicenseCheckResponse> {
        val did = getDeviceIdentifier()
        val lcr = LicenseCheckRequest(did, token)

        return runCatching {
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(lcr)
            }
                .body<LicenseCheckResponse>()
        }
    }


    fun validateJwt(token: String, aud: String, base64PublicKey: String): Boolean {
        try {
            val parts = token.split(".")
            if (parts.size != 3) return false

            val header = parts[0].decodeBase64String().let {
                Json.decodeFromString<JwtHeader>(it)
            }

            if (!(header.alg == "RS256" && header.typ == "JWT"))
                return false

            val payload = parts[1].decodeBase64String().let {
                Json.decodeFromString<JwtPayload>(it)
            }

            if (!(payload.aud == aud && payload.nbf < System.currentTimeMillis() / 1000 && payload.exp > System.currentTimeMillis() / 1000))
                return false

            val signature = parts[2].decodeBase64Bytes()

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
        publicKey: PublicKey
    ): Boolean {
        return try {
            val sig = Signature.getInstance(JWT_SIGNATURE_ALGORITHM)
            sig.initVerify(publicKey)
            sig.update(data)
            sig.verify(signature)
        } catch (e: Exception) {
            false
        }
    }


    private fun getDeviceIdentifier(): String {
        val name = Build.BRAND + "|" + Build.MODEL + "|" + Build.DEVICE + "|" + Build.BOARD
        return name.sha256()
    }

    private fun String.sha256() =
        MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .encodeBase64()

}