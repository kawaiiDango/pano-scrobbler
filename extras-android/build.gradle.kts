plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.arn.scrobble.utils.android"
    compileSdk {
        version = release(libs.versions.targetSdk.get().toInt()) {
//            minorApiLevel = libs.versions.sdkMinor.get().toInt()
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
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(projects.extrasCommon)
    releaseImplementation(projects.extrasPlay)
//    debugImplementation(projects.extrasNonplay)
    debugImplementation(projects.extrasPlay)
    "releaseGithubImplementation"(projects.extrasNonplay)
}
