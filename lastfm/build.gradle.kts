plugins {
    id("com.android.library")
}

android {
    namespace = "de.umass.lastfm"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
    }

    resourcePrefix = "lastfm_"
}

dependencies {
    implementation("androidx.annotation:annotation:1.4.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.10")
}