// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        mavenCentral()
    }

    dependencies {
        classpath(libs.play.publisher)
    }
}

plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.navigation.safeargs) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.test) apply false
    alias(libs.plugins.crashlytics) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.github.release) apply false
    alias(libs.plugins.baselineprofile) apply false
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

// play store and github publishing scripts
// remove if not needed

tasks.register<GradleBuild>("cleanBuildDraft") {
    tasks = listOf(
        "clean",
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