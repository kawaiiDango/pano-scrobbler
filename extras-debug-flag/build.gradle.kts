plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.arn.scrobble.debugflag"
    compileSdk = libs.versions.targetSdk.get().toInt()

    buildFeatures {
        buildConfig = true
        aidl = false
        renderScript = false
        resValues = false
        shaders = false
    }

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildTypes {
        getByName("release") {
        }

        getByName("debug") {
        }

        create("releaseGithub") {
            initWith(getByName("release"))
        }
    }
}