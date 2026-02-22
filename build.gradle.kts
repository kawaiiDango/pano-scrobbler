import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

extra.apply {
    val versionFile = file("version.txt")
    val verCode = versionFile.readText().trim().toInt()
    set("VER_CODE", verCode)
    set("VER_NAME", "${verCode / 100}.${verCode % 100}")
    set("BUILD_DATE", SimpleDateFormat("yyyy, MMM dd", Locale.ENGLISH).format(Date()))
    set("APP_ID", "com.arn.scrobble")
    set("APP_NAME", "Pano Scrobbler")
    set("APP_NAME_NO_SPACES", "pano-scrobbler")
}
