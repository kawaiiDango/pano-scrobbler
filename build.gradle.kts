plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.crashlytics) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.test) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
}

val os = org.gradle.internal.os.OperatingSystem.current()!!
val arch = System.getProperty("os.arch")!!

val archAmd64 = arrayOf("amd64", "x86_64")
val archArm64 = arrayOf("aarch64", "arm64")

val resourcesDirName = when {
    os.isLinux && arch in archAmd64 -> "linux-x64"
    os.isLinux && arch in archArm64 -> "linux-arm64"
    os.isWindows && arch in archAmd64 -> "windows-x64"
    os.isWindows && arch in archArm64 -> "windows-arm64"
    else -> throw IllegalStateException("Unsupported platform: $os $arch")
}

extra.apply {
    val versionFile = file("version.txt")
    val verCode = versionFile.readText().trim().toInt()
    set("VER_CODE", verCode)
    set("VER_NAME", "${verCode / 100}.${verCode % 100}")
    set("APP_ID", "com.arn.scrobble")
    set("APP_NAME", "Pano Scrobbler")
    set("APP_NAME_NO_SPACES", "pano-scrobbler")
    set("RESOURCES_DIR_NAME", resourcesDirName)
    set("IS_WINDOWS", os.isWindows)
    set("IS_LINUX", os.isLinux)
    set("IS_X64", arch in archAmd64)
    set("IS_ARM64", arch in archArm64)
}
