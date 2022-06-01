plugins {
    id("com.android.library")
}

android {
    compileSdk = 32

    defaultConfig {
        minSdk = 21
        targetSdk = 32
    }

    resourcePrefix = "spark_"
}

dependencies {
    implementation("androidx.annotation:annotation:1.3.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.6.0")
}