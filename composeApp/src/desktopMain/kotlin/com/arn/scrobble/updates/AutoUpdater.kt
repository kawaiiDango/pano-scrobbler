package com.arn.scrobble.updates

import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.github.GithubReleases
import com.arn.scrobble.utils.DesktopStuff
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object AutoUpdater {

    // downloads the .appimage from Github, verifies the SHA256 checksum,
    // and replaces the current app with the new version
    // returns if it was successful
    private suspend fun linux(
        downloadUrl: String,
        sha256Hex: String,
    ): File? {
        val appImagePath = System.getenv("APPIMAGE")
        if (appImagePath == null) {
            Logger.i { "Not an AppImage, skipping update" }
            return null
        }
        val appImageFile = File(appImagePath)
        val parentDir = appImageFile.parentFile
        val tempFile = File(parentDir, appImageFile.name + ".new")

        if (tempFile.exists()) {
            tempFile.delete()
        }

        val downloaded = download(downloadUrl, tempFile)

        if (!downloaded) {
            return null
        }

        delay(5000)

        if (!verify(tempFile, sha256Hex)) {
            tempFile.delete()
            return null
        }

        tempFile.setExecutable(true)

        // 2. Replace old AppImage
        Logger.i { "Replacing old AppImage" }
        val backupFile = File(parentDir, appImageFile.name + ".bak")
        appImageFile.copyTo(backupFile, overwrite = true)
        try {
            Files.move(
                tempFile.toPath(),
                appImageFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            Logger.i("Update applied!")
        } catch (e: Exception) {
            Logger.e("Error replacing AppImage: ${e.message}")
            // Optionally restore backup
            backupFile.copyTo(appImageFile, overwrite = true)
            tempFile.delete()
            return null
        } finally {
            backupFile.delete()
        }

        return appImageFile
    }

    // downloads the .exe installer from Github, verifies the SHA256 checksum,
    // returns if it was successful
    private suspend fun windows(
        downloadUrl: String,
        sha256Hex: String,
    ): File? {
        val targetFile = File(DesktopStuff.appDataRoot, "updates\\pano-scrobbler-update.exe")

        if (targetFile.exists()) {
            if (verify(targetFile, sha256Hex))
                return targetFile
            else
                targetFile.delete()
        } else {
            targetFile.parentFile.mkdirs()
        }

        val downloaded = download(downloadUrl, targetFile)

        if (!downloaded) {
            return null
        }

        delay(5000)

        if (!verify(targetFile, sha256Hex)) {
            targetFile.delete()
            return null
        }

        return targetFile
    }

    suspend fun download(
        downloadUrl: String,
        destinationFile: File
    ): Boolean {
        Logger.i { "Downloading update" }

        return withContext(Dispatchers.IO) {
            Requesters.genericKtorClient.prepareGet(downloadUrl) {
                timeout {
                    requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                }
            }
                .execute { response ->
                    if (!response.status.isSuccess()) {
                        Logger.e { "Failed to download update: ${response.status}" }
                        return@execute false
                    }

                    runCatching {
                        response.bodyAsChannel()
                            .copyAndClose(destinationFile.writeChannel())
                    }.onFailure { e ->
                        Logger.e { "Error writing update to file: ${e.message}" }
                        return@execute false
                    }

                    return@execute true
                }
        }
    }

    suspend fun verify(
        destinationFile: File,
        sha256Hex: String,
    ): Boolean {
        // verify SHA256 checksum
        Logger.i { "Verifying update" }

        val actualSha256 = withContext(Dispatchers.IO) {
            destinationFile.inputStream().use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        }

        if (actualSha256 != sha256Hex) {
            Logger.e { "SHA256 checksum mismatch: expected $sha256Hex, got $actualSha256" }
            return false
        }

        return true
    }

    suspend fun update(
        releases: GithubReleases,
    ): File? {
        val downloadUrl = releases.getDownloadUrl(BuildKonfig.OS_ARCH)
        if (downloadUrl.isEmpty()) {
            return null
        }

        val file = when (DesktopStuff.os) {
            DesktopStuff.Os.Windows -> {
                val asset = downloadUrl.find { it.name.endsWith(".exe") }
                    ?: return null // No Windows installer found
                val sha256Hex = asset.digest.substringAfter("sha256:")
                windows(asset.browser_download_url, sha256Hex)
            }

            DesktopStuff.Os.Linux -> {
                val asset = downloadUrl.find { it.name.endsWith(".AppImage") }
                    ?: return null // No Linux AppImage found
                val sha256Hex = asset.digest.substringAfter("sha256:")
                linux(asset.browser_download_url, sha256Hex)
            }

            else -> {
                null // MacOS not supported
            }
        }


        var waitTime = 5.minutes
        val step = 30.seconds

        // this is needed on Windows while Defender is scanning the file
        if (file != null) {
            while (PanoNativeComponents.isFileLocked(file.absolutePath) && waitTime > 0.seconds) {
                Logger.i { "Update file is locked" }
                delay(step)
                waitTime -= step
            }
        }

        return file
    }
}