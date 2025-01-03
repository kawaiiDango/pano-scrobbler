plugins {
    alias(libs.plugins.android.library)
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

//    kotlinOptions {
//        jvmTarget = "21"
//    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.core)

    implementation(libs.billing)
    implementation(libs.review)
    implementation(libs.kermit)
    implementation(platform(libs.firebase.bom))
    implementation(libs.crashlytics)
    implementation(projects.extrasCommon)
}