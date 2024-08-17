package com.arn.scrobble.billing

/**
 * This class is an addendum. It shouldn't really be here: it should be on the secure server. But if
 * a secure server does not exist, it's still good to check the signature of the purchases coming
 * from Google Play. At the very least, it will combat man-in-the-middle attacks. Putting it
 * on the server would provide the additional protection against hackers who may
 * decompile/rebuild the app.
 *
 * Sigh... All sorts of attacks can befall your app, website, or platform. So when it comes to
 * implementing security measures, you have to be realistic and judicious so that user experience
 * does not suffer needlessly. And you should analyze that the money you will save (minus cost of labor)
 * by implementing security measure X is greater than the money you would lose if you don't
 * implement X. Talk to a UX designer if you find yourself obsessing over security.
 *
 * The good news is, in implementing [BillingRepository], a number of measures is taken to help
 * prevent fraudulent activities in your app. We don't just focus on tech savvy hackers, but also
 * on fraudulent users who may want to exploit loopholes. Just to name an obvious case:
 * triangulation using Google Play, your secure server, and a local cache helps against non-techie
 * frauds.
 */
import android.content.pm.PackageManager
import com.android.billingclient.api.Purchase
import com.arn.scrobble.Tokens
import com.arn.scrobble.main.App
import com.arn.scrobble.utils.Stuff
import io.ktor.util.decodeBase64Bytes
import io.ktor.util.decodeBase64String
import kotlinx.serialization.Serializable
import java.io.IOException
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

/**
 * Security-related methods. For a secure implementation, all of this code should be implemented on
 * a server that communicates with the application on the device.
 */
object Security {
    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val PLAY_SIGNATURE_ALGORITHM = "SHA1withRSA"
    private const val JWT_SIGNATURE_ALGORITHM = "SHA256withRSA"

    /**
     * Verifies that the data was signed with the given signature
     *
     * @param base64PublicKey the base64-encoded public key to use for verifying.
     * @param signedData the signed JSON string (signed, not encrypted)
     * @param signature the signature for the data, signed with the private key
     * @throws IOException if encoding algorithm is not supported or key specification
     * is invalid
     */
    @Throws(IOException::class)
    fun verifyPurchase(purchase: Purchase): Boolean {
        if (purchase.originalJson.isEmpty() || Tokens.BASE_64_ENCODED_PUBLIC_KEY.isEmpty() ||
            purchase.signature.isEmpty() || signature() == null
        ) {
            return false
        }
        val key = loadPublicKey(Tokens.BASE_64_ENCODED_PUBLIC_KEY)
        return verify(key, purchase.originalJson, purchase.signature)
    }

    /**
     * Generates a PublicKey instance from a string containing the Base64-encoded public key.
     *
     * @param encodedPublicKey Base64-encoded public key
     * @throws IOException if encoding algorithm is not supported or key specification
     * is invalid
     */
    @Throws(IOException::class)
    private fun loadPublicKey(encodedPublicKey: String): PublicKey {
        try {
            val decodedKey = encodedPublicKey.decodeBase64Bytes()
            val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            return keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: NoSuchAlgorithmException) {
            // "RSA" is guaranteed to be available.
            throw RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            throw IOException(e)
        }
    }

    /**
     * Verifies that the signature from the server matches the computed signature on the data.
     * Returns true if the data is correctly signed.
     *
     * @param publicKey public key associated with the developer account
     * @param signedData signed data from server
     * @param signature server signature
     * @return true if the data and signature match
     */
    private fun verify(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        val signatureBytes: ByteArray
        try {
            signatureBytes = signature.decodeBase64Bytes()
        } catch (e: IllegalArgumentException) {
            return false
        }
        try {
            val signatureAlgorithm = Signature.getInstance(PLAY_SIGNATURE_ALGORITHM)
            signatureAlgorithm.initVerify(publicKey)
            signatureAlgorithm.update(signedData.toByteArray())
            return signatureAlgorithm.verify(signatureBytes)
        } catch (e: NoSuchAlgorithmException) {
            // "RSA" is guaranteed to be available.
            throw RuntimeException(e)
        } catch (_: InvalidKeyException) {
        } catch (_: SignatureException) {
        }
        return false
    }

    fun signature(): String? {
        try {
            val signatures = App.context.packageManager.getPackageInfo(
                App.context.packageName,
                PackageManager.GET_SIGNATURES
            ).signatures
            val signature = signatures?.get(0)?.toCharsString()
            if (signature == Tokens.SIGNATURE)
                return signature
        } catch (ex: Exception) {
        }
        android.os.Process.killProcess(android.os.Process.myPid())
        return null
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun sha256(s: String) =
        MessageDigest.getInstance("SHA-256")
            .digest(s.toByteArray())
            .toHexString()

    fun validateJwt(token: String, base64PublicKey: String): Boolean {
        @Serializable
        data class JwtHeader(val alg: String, val typ: String)

        try {
            val parts = token.split(".")
            if (parts.size != 3) return false

            val header = parts[0].decodeBase64String().let {
                Stuff.myJson.decodeFromString<JwtHeader>(it)
            }

            if (header.alg != "RS256") return false
//            val payload = String(Base64.decode(parts[1], Base64.DEFAULT))
            val signature = parts[2].decodeBase64Bytes()

            val data = "${parts[0]}.${parts[1]}".toByteArray()

            val trimmedKey = base64PublicKey.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s".toRegex(), "")

            val publicKey = loadPublicKey(trimmedKey)
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
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(publicKey)
            sig.update(data)
            sig.verify(signature)
        } catch (e: Exception) {
            false
        }
    }

}