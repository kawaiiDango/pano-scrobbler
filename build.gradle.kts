// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    val kotlin_version by extra("1.8.21")
    repositories {
        google()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        mavenCentral()
    }

    dependencies {
        classpath("com.github.triplet.gradle:play-publisher:4.0.0-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.6.0-rc01")
    }
}

plugins {
    id("com.android.application") version "8.2.0-alpha04" apply false
    kotlin("android") version "1.8.21" apply false
    id("com.google.firebase.crashlytics") version "2.9.5" apply false
    id("com.google.gms.google-services") version "4.3.15" apply false
    id("com.google.devtools.ksp") version "1.8.21-1.0.11" apply false
    kotlin("plugin.serialization") version "1.8.21" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "10.7.0" apply false
    id("com.github.breadmoirai.github-release") version "2.4.1" apply false
    id("com.android.test") version "8.2.0-alpha04" apply false
    id("androidx.baselineprofile") version "1.2.0-alpha13" apply false
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