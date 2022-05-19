plugins {
    id("com.android.library")
}

android {
    compileSdk = 32

    defaultConfig {
        minSdk = 21
        targetSdk = 32
    }

    resourcePrefix = "lastfm_"
}

dependencies {
    implementation("androidx.annotation:annotation:1.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
}