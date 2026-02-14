plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm("desktop")

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    android {
        namespace = "com.arn.scrobble.extras.common"
        compileSdk = libs.versions.targetSdk.get().toInt()
//        compileSdkPreview = "CinnamonBun"
        minSdk = libs.versions.minSdk.get().toInt()
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        val desktopMain by getting

        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }


        desktopMain.dependencies {
        }
    }

}