plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.arn.scrobble.extras.foss"
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
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.core)

    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)
//    implementation(libs.okhttp)
//    implementation(libs.ktor.client.okhttp)
//    implementation(libs.ktor.serialization.kotlinx.json)
//    implementation(libs.ktor.client.auth)
//    implementation(libs.ktor.client.content.negotiation)
    implementation(project(":extras-common"))

}