import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.StrictMode
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.github.release)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.play.publisher)
    id(libs.plugins.parcelize.get().pluginId)
}

android {

    val versionFile = file("version.txt")
    var verCode: Int
    if (versionFile.canRead()) {
        verCode = versionFile.readText().toInt()
        for (task in gradle.startParameter.taskNames) {
            if (task.contains("publish")) {
                verCode++
                versionFile.writeText(verCode.toString())
                break
            }
        }
    } else {
        throw GradleException("Could not read version.txt!")
    }

    compileSdk = 34
    defaultConfig {
        applicationId = "com.arn.scrobble"
        namespace = "com.arn.scrobble"
        minSdk = 23
        targetSdk = 34
        versionCode = verCode
        versionName = "${verCode / 100}.${verCode % 100} - ${
            SimpleDateFormat("YYYY, MMM dd").format(Date())
        }"
        setProperty("archivesBaseName", "pano-scrobbler")
        vectorDrawables.useSupportLibrary = true

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.generateKotlin", "true")
            arg("room.incremental", "true")
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            )
        }

        debug {
            versionNameSuffix = " DEBUG"
//            extra["enableCrashlytics"] = false
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        freeCompilerArgs += "-Xjvm-default=all"
        jvmTarget = "17"
    }
}

aboutLibraries {
    configPath = "aboutLibsConfig"
    offlineMode = true
    fetchRemoteLicense = false
    fetchRemoteFunding = false
    excludeFields = arrayOf(
        "developers",
        "funding",
        "description",
        "organization",
        "content",
        "connection",
        "developerConnection"
    )
    strictMode = StrictMode.FAIL
    duplicationMode = DuplicateMode.MERGE
}

//tasks.withType(KaptGenerateStubsTask::class.java).configureEach {
//    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
//}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("acrcloud*.jar"))))
    debugImplementation(fileTree(mapOf("dir" to "libs", "include" to listOf("androidjhlabs.jar"))))

    implementation(libs.profileinstaller)
    "baselineProfile"(project(mapOf("path" to ":baselineprofile")))

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.kotlin.stdlib)
    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.preference.ktx)
    implementation(libs.media)
    implementation(libs.palette.ktx)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.core.remoteviews)
    implementation(libs.androidx.transition)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.billing)
    implementation(libs.review.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.android.snowfall)
    // viewpager2 doesnt respond to left/right press on TVs, don"t migrate

    implementation(libs.material)
    implementation(libs.timber)
    implementation(platform(libs.firebase.bom))
    // Declare the dependencies for the Crashlytics and Analytics libraries
    // When using the BoM, you don"t specify versions in Firebase library dependencies
    implementation(libs.crashlytics.ktx)

    implementation(libs.okhttp)
    implementation(libs.krate)
    implementation(libs.krate.kotlinx)
    implementation(libs.harmony)
    implementation(libs.kumo.core)
    implementation(libs.bimap)
    implementation(libs.mpAndroidChart)
    implementation(libs.nestedscrollwebview)
    implementation(libs.skeletonlayout)
    implementation(libs.coil)
    implementation(libs.coil.gif)

    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.content.negotiation)
    debugImplementation(libs.ktor.client.logging)
    debugImplementation(libs.slf4j.simple)

    implementation(libs.aboutlibraries.core)
//    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.espresso.core)
}


// play store and github publishing scripts
// remove if not needed

val localProperties = gradleLocalProperties(rootDir, project.providers) as Map<String, String>

android {
    signingConfigs {
        register("release") {
            storeFile = file(localProperties["release.keystore"]!!)
            storePassword = localProperties["release.storePassword"]
            keyAlias = localProperties["release.alias"]
            keyPassword = localProperties["release.password"]
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

play {
    track.set("beta") // or 'rollout' or 'beta' or 'alpha'
//    userFraction = 1.0d // only necessary for 'rollout', in this case default is 0.1 (10% of the target)
    defaultToAppBundles.set(true)
    serviceAccountCredentials.set(file("play_api_keys.json"))

    fromTrack.set("beta")
    promoteTrack.set("production")
}

githubRelease {
    token(localProperties["github.token"])
    owner("kawaiidango")
    repo("pano-scrobbler")
    val changelog = file("src/main/play/release-notes/en-US/default.txt").readText() +
            "\n\n" + "Copied from Play Store what's new, may not be accurate for minor updates."
    body(changelog)
    tagName(android.defaultConfig.versionCode.toString())
    releaseName(android.defaultConfig.versionName)
    targetCommitish("main")
    releaseAssets(
        listOf(
            "build/outputs/apk/release/pano-scrobbler-release.apk",
        )
    )
    draft(false) // by default this is true
    allowUploadToExisting(false) // Setting this to true will allow this plugin to upload artifacts to a release if it found an existing one. If overwrite is set to true, this option is ignored.
    overwrite(false) // by default false; if set to true, will delete an existing release with the same tag and name
    dryRun(false) // by default false; you can use this to see what actions would be taken without making a release
}