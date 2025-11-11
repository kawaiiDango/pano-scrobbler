import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.android.application)
//    alias(libs.plugins.baselineprofile)
//    alias(libs.plugins.play.publisher)
}

if (gradle.startParameter.taskNames.none { it.contains("releaseGithub", ignoreCase = true) }) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.crashlytics.get().pluginId)
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

    defaultConfig {
        applicationId = APP_ID
        namespace = APP_ID + ".androidApp"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = VER_CODE
        versionName = VER_NAME
        base.archivesName = APP_NAME_NO_SPACES
    }

    buildFeatures {
        aidl = false
        buildConfig = false
        renderScript = false
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
        }

        jniLibs {
            useLegacyPackaging = false
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

//    baselineProfile {
//        dexLayoutOptimization = true
//    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }

    dependencies {
        implementation(projects.composeApp)
        implementation(libs.androidx.work.runtime)
        implementation(projects.extrasCommon)


//        "baselineProfile"(project(mapOf("path" to ":baselineprofile")))

        releaseImplementation(projects.extrasPlay)
//        debugImplementation(projects.extrasPlay)
        debugImplementation(projects.extrasNonplay)
        "releaseGithubImplementation"(projects.extrasNonplay)

        androidTestImplementation(libs.androidx.uiautomator)
        androidTestImplementation(libs.androidx.runner)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(libs.androidx.test.ext.junit)
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
        }

        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
        }

        getByName("releaseGithub") {
            signingConfig = signingConfigs.findByName("releaseGithub")
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

afterEvaluate {
    tasks.named("packageReleaseGithub") {
        finalizedBy(tasks.named("copyGithubReleaseApk"))
    }
}

//play {
//    track.set("beta") // or 'rollout' or 'beta' or 'alpha'
////    userFraction = 1.0d // only necessary for 'rollout', in this case default is 0.1 (10% of the target)
//    defaultToAppBundles.set(true)
//    serviceAccountCredentials.set(file("play_api_keys.json"))
//
//    fromTrack.set("beta")
//    promoteTrack.set("production")
//}