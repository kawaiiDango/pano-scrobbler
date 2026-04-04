import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.arn.scrobble.utils.android"
    compileSdk {
        version = release(libs.versions.targetSdk.get().toInt()) {
            minorApiLevel = libs.versions.sdkMinor.get().toInt()
        }
    }

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }

    buildTypes {
        create("releaseGithub") {
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(projects.extrasCommon)
    releaseImplementation(projects.extrasPlay)
//    debugImplementation(projects.extrasNonplay)
    debugImplementation(projects.extrasPlay)
    "releaseGithubImplementation"(projects.extrasNonplay)
}
