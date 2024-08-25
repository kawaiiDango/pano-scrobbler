package com.arn.scrobble.billing

import android.content.Context
import android.content.pm.PackageManager
import io.ktor.util.decodeBase64Bytes
import java.io.IOException
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

/**
 * Security-related methods. For a secure implementation, all of this code should be implemented on
 * a server that communicates with the application on the device.
 */
object Security {
    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val PLAY_SIGNATURE_ALGORITHM = "SHA1withRSA"

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
    fun verifyPurchase(
        originalJson: String,
        jsonSignature: String,
        publicKeyBase64: String,
    ): Boolean {
        if (originalJson.isEmpty() || publicKeyBase64.isEmpty() ||
            jsonSignature.isEmpty()
        ) {
            return false
        }
        val key = loadPublicKey(publicKeyBase64)
        return verify(key, originalJson, jsonSignature)
    }

    /**
     * Generates a PublicKey instance from a string containing the Base64-encoded public key.
     *
     * @param encodedPublicKey Base64-encoded public key
     * @throws IOException if encoding algorithm is not supported or key specification
     * is invalid
     */
    @Throws(IOException::class)
    fun loadPublicKey(encodedPublicKey: String): PublicKey {
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

    fun signature(context: Context, signatureBase64: String): String? {
        try {
            val signatures = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            ).signatures
            val signature = signatures?.get(0)?.toCharsString()
            if (signature == signatureBase64)
                return signature
        } catch (ex: Exception) {
        }
        android.os.Process.killProcess(android.os.Process.myPid())
        return null
    }
}