plugins {
    alias(libs.plugins.android.library)
}

kotlin {
    jvmToolchain(25)
}

android {
    namespace = "com.arn.scrobble.extras.play"
    compileSdk {
        version = release(libs.versions.targetSdk.get().toInt()) {
//            minorApiLevel = libs.versions.sdkMinor.get().toInt()
        }
    }

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        consumerProguardFiles("consumer-rules.pro")
    }
    lint {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }
    buildTypes {
        create("releaseGithub") {
            matchingFallbacks += listOf("release")
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.core)
    implementation(libs.startup)

    implementation(libs.play.services.base)
    implementation(libs.billing)
    implementation(libs.review)
    implementation(libs.kermit)
    implementation(platform(libs.firebase.bom))
    implementation(libs.crashlytics)
    implementation(projects.extrasCommon)
}

configurations.all {
    // play services pulls this
    exclude(group = "androidx.fragment", module = "fragment")
}