plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
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

    kotlin {
        jvmToolchain(25)
    }

    lint {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    buildTypes {
        create("releaseGithub") {
        }
    }

    buildFeatures {
        aidl = true
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(projects.extrasCommon)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kermit)
    releaseImplementation(projects.extrasPlay)
//    debugImplementation(projects.extrasNonplay)
    debugImplementation(projects.extrasPlay)
    "releaseGithubImplementation"(projects.extrasNonplay)
}
