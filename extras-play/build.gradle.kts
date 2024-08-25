plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.arn.scrobble.extras.play"
    compileSdk = libs.versions.targetSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        create("releaseGithub") {
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.core)

    implementation(libs.billing)
    implementation(libs.review)
    implementation(libs.timber)
    implementation(platform(libs.firebase.bom))
    implementation(libs.crashlytics)
    implementation(project(":extras-common"))
}