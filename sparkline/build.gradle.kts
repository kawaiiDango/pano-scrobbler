plugins {
    id("com.android.library")
}

android {
    namespace = "com.robinhood.spark"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
    }

    resourcePrefix = "spark_"
}

dependencies {
    implementation("androidx.annotation:annotation:1.5.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.8.1")
}