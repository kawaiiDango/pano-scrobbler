import io.gitlab.arturbosch.detekt.Detekt

// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.navigation.safeargs) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.crashlytics) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.test) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.github.release) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
}

detekt {
    toolVersion = "1.23.3"
//    config.setFrom(file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

tasks.withType<Detekt>().configureEach {
    reports {
        xml.required.set(false)
        html.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}


tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
}

// play store and github publishing scripts
// remove if not needed

tasks.register<GradleBuild>("cleanBuildDraft") {
    tasks = listOf(
        "clean",
        "genDict",
        "fetchCrowdinMembers",
        "publishReleaseBundle",
        "assembleRelease",
    )
}

tasks.register<GradleBuild>("finalizeDraft") {
    tasks = listOf(
        "promoteArtifact",
        "githubRelease",
    )
}