// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    val kotlin_version by extra("1.8.10")
    repositories {
        google()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        mavenCentral()
    }

    dependencies {
        classpath("com.github.triplet.gradle:play-publisher:4.0.0-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.6.0-alpha06")
    }
}

plugins {
    id("com.android.application") version "8.1.0-alpha09" apply false
    kotlin("android") version "1.8.0" apply false
    id("com.google.firebase.crashlytics") version "2.9.2" apply false
    id("com.google.gms.google-services") version "4.3.15" apply false
    id("com.google.devtools.ksp") version "1.8.10-1.0.9" apply false
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