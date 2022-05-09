// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    dependencies {
        classpath("com.github.triplet.gradle:play-publisher:4.0.0-SNAPSHOT")
    }
}

plugins {
    id("com.android.application") version "7.3.0-alpha09" apply false
//    id("com.android.application") version "7.1.3" apply false
    kotlin("android") version "1.6.20" apply false
    id("com.google.firebase.crashlytics") version "2.8.1" apply false
    id("com.google.gms.google-services") version "4.3.10" apply false
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

// play store and github publishing scripts
// remove if not needed

tasks.register<GradleBuild>("cleanBuildPublish") {
    tasks = listOf(
        "clean",
        "publishReleaseBundle",
        "assembleRelease",
        "githubRelease",
    )
}