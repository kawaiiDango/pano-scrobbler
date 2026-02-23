import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.StrictMode

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.baselineprofile)
}

val requestedTasks = gradle.startParameter.taskNames.map { it.lowercase() }

if (requestedTasks.none { it.contains("releasegithub") }) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.crashlytics.get().pluginId)
}

val aboutLibrariesVariant = when {
    requestedTasks.any { it.contains("releasegithub") } -> "releaseGithub"
    requestedTasks.any { it.contains("release") } -> "release"
    else -> null
}

val APP_ID: String by rootProject.extra
val VER_CODE: Int by rootProject.extra
val VER_NAME: String by rootProject.extra
val APP_NAME: String by rootProject.extra
val APP_NAME_NO_SPACES: String by rootProject.extra

val localProperties = gradleLocalProperties(rootDir, project.providers)
    .map { it.key to it.value.toString() }
    .toMap()

android {
    compileSdk = libs.versions.targetSdk.get().toInt()
//    compileSdkPreview = "CinnamonBun"

    defaultConfig {
        applicationId = APP_ID
        namespace = APP_ID + ".androidApp"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
//        targetSdkPreview = "CinnamonBun"
        versionCode = VER_CODE
        versionName = VER_NAME
        base.archivesName = APP_NAME_NO_SPACES
//        ndkVersion = "29.0.14206865"
    }

    buildFeatures {
        aidl = false
        buildConfig = true
        resValues = false
        shaders = false
    }

    buildTypes {
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
            excludes.add("/composeResources/**") // this is a duplicate. idk why this gets added.
            excludes.add("/META-INF/**/*.txt")
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
            signingConfig = signingConfigs.findByName("releaseGithub")
//            signingConfig = signingConfigs.findByName("release")
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
    implementation(libs.work.runtime)
    implementation(projects.extrasCommon)


//        "baselineProfile"(project(mapOf("path" to ":baselineprofile")))

    releaseImplementation(projects.extrasPlay)
    debugImplementation(projects.extrasNonplay)
//    debugImplementation(projects.extrasPlay)
    "releaseGithubImplementation"(projects.extrasNonplay)

    androidTestImplementation(libs.test.uiautomator)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.espresso.core)
    androidTestImplementation(libs.test.junit)
}

baselineProfile {
    dexLayoutOptimization = true
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