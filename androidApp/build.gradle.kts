import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import com.google.gms.googleservices.GoogleServicesPlugin
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.StrictMode

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
}

val requestedTasks = gradle.startParameter.taskNames.map { it.lowercase() }

val aboutLibrariesVariant = when {
    requestedTasks.any { it.contains("releasegithub") } -> "releaseGithub"
    requestedTasks.any { it.contains("release") } -> "release"
    else -> null
}

val APP_ID = rootProject.extra["APP_ID"] as String
val VER_CODE = rootProject.extra["VER_CODE"] as Int
val VER_NAME = rootProject.extra["VER_NAME"] as String
val APP_NAME = rootProject.extra["APP_NAME"] as String
val APP_NAME_NO_SPACES = rootProject.extra["APP_NAME_NO_SPACES"] as String

val localProperties = gradleLocalProperties(rootDir, project.providers)
    .map { it.key to it.value.toString() }
    .toMap()

android {
    buildToolsVersion = "37.0.0"

    compileSdk {
        version = release(libs.versions.targetSdk.get().toInt()) {
            minorApiLevel = libs.versions.sdkMinor.get().toInt()
        }
    }

    defaultConfig {
        applicationId = APP_ID
        namespace = APP_ID + ".androidApp"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = VER_CODE
        versionName = VER_NAME
        base.archivesName = APP_NAME_NO_SPACES
//        ndkVersion = "29.0.14206865"
    }

    buildFeatures {
        aidl = false
        resValues = false
        shaders = false
    }

    buildTypes {
        all {
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = (name == "release")
            }
        }

        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            versionNameSuffix = " DEBUG"
        }

        create("releaseGithub") {
            initWith(getByName("release"))
            versionNameSuffix = " GH"
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/**/*.txt")
            excludes.add("/META-INF/native-image/**")
            excludes.add("DebugProbesKt.bin")
        }

        jniLibs {
            useLegacyPackaging = false
            // remove the need for heavyweight ndk
            keepDebugSymbols.add("**/*.so")
        }

        bundle {
            language {
                enableSplit = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }



    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }

    signingConfigs {
        if (
            localProperties["release.keystore"] != null &&
            localProperties["release.storePassword"] != null &&
            localProperties["release.alias"] != null &&
            localProperties["release.password"] != null
        ) {
            register("release") {
                storeFile = file(localProperties["release.keystore"]!!)
                storePassword = localProperties["release.storePassword"]
                keyAlias = localProperties["release.alias"]
                keyPassword = localProperties["release.password"]
            }
        }

        if (
            localProperties["releaseGithub.keystore"] != null &&
            localProperties["releaseGithub.storePassword"] != null &&
            localProperties["releaseGithub.alias"] != null &&
            localProperties["releaseGithub.password"] != null
        ) {
            register("releaseGithub") {
                storeFile = file(localProperties["releaseGithub.keystore"]!!)
                storePassword = localProperties["releaseGithub.storePassword"]
                keyAlias = localProperties["releaseGithub.alias"]
                keyPassword = localProperties["releaseGithub.password"]
            }
        }
    }

    buildTypes {
        getByName("debug") {
//            signingConfig = signingConfigs.findByName("releaseGithub")
            signingConfig = signingConfigs.findByName("release")
        }

        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
        }

        getByName("releaseGithub") {
            signingConfig = signingConfigs.findByName("releaseGithub")
        }
    }
}

dependencies {
    implementation(projects.composeApp)
//        "baselineProfile"(project(mapOf("path" to ":baselineprofile")))

    androidTestImplementation(libs.test.uiautomator)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(libs.test.junit)
}

baselineProfile {
    dexLayoutOptimization = true
}

googleServices {
    // 'releaseGithub' variant does not need a google-services.json
    missingGoogleServicesStrategy = GoogleServicesPlugin.MissingGoogleServicesStrategy.WARN
}

aboutLibraries {
    offlineMode = true

    collect {
        configPath = File("../aboutLibsConfig")
        fetchRemoteLicense = false
        fetchRemoteFunding = false
        license.strictMode = StrictMode.WARN
        library.duplicationMode = DuplicateMode.MERGE
    }

    export {
        excludeFields = listOf(
            "developers",
            "funding",
            "description",
            "organization",
            "content",
            "connection",
            "developerConnection"
        )

        variant = aboutLibrariesVariant
    }

    exports {
        create("release") {
            outputFile =
                file("../composeApp/src/androidMain/composeResources/files/aboutlibraries.json")
        }
        create("releaseGithub") {
            outputFile =
                file("../composeApp/src/androidMain/composeResources/files/aboutlibraries.json")
        }
    }

}

tasks.register<Copy>("copyGithubReleaseApk") {
    from("build/outputs/apk/releaseGithub")
    into("../dist")
    include("*-releaseGithub.apk")
    rename(
        "(.*)-releaseGithub.apk",
        "$APP_NAME_NO_SPACES-android-universal.apk"
    )
}

tasks.configureEach {
    if (name == "packageReleaseGithub") {
        finalizedBy("copyGithubReleaseApk")
    }
}