plugins {
    id("com.android.library")
}

android {
    namespace = "de.umass.lastfm"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.6.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
}