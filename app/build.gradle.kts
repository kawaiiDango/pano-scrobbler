import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.StrictMode
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-parcelize")
    id("com.github.triplet.play")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.devtools.ksp")
    kotlin("plugin.serialization")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.github.breadmoirai.github-release")
}

android {

    val versionFile = file("version.txt")
    var verCode: Int
    if (versionFile.canRead()) {
        verCode = versionFile.readText().toInt()
        for (task in gradle.startParameter.taskNames) {
            if (task.contains("publishReleaseBundle") || task.contains("publish")) {
                verCode++
                versionFile.writeText(verCode.toString())
                break
            }
        }
    } else {
        throw GradleException("Could not read version.txt!")
    }

    compileSdk = 33
//    compileSdkPreview = "UpsideDownCake"
    defaultConfig {
        applicationId = "com.arn.scrobble"
        namespace = "com.arn.scrobble"
        minSdk = 21
        targetSdk = 33
//        targetSdkPreview = "UpsideDownCake"
        versionCode = verCode
        versionName = "${verCode / 100}.${verCode % 100} - ${
            SimpleDateFormat("YYYY, MMM dd").format(Date())
        }"
        setProperty("archivesBaseName", "pScrobbler")
        vectorDrawables.useSupportLibrary = true

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }
    buildFeatures {
        viewBinding = true
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
//            applicationIdSuffix = ".debug"
//            extra["enableCrashlytics"] = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        freeCompilerArgs += "-Xjvm-default=all"
    }

    kotlin {
        jvmToolchain(17)
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

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:1.7.0-alpha02")
    implementation("androidx.core:core-ktx:1.10.0-rc01")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.media:media:1.6.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
//    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0-alpha08")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0-alpha07")
    implementation("androidx.navigation:navigation-ui-ktx:2.6.0-alpha07")
    implementation("androidx.core:core-remoteviews:1.0.0-beta03")
    ksp("androidx.room:room-compiler:2.5.0")
    implementation("androidx.room:room-runtime:2.5.0")
    implementation("com.android.billingclient:billing:5.1.0")
    implementation("com.google.android.play:review:2.0.1")
    implementation("com.google.android.play:review-ktx:2.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    // viewpager2 doesnt respond to left/right press on TVs, don"t migrate

    implementation("com.google.android.material:material:1.9.0-beta01")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation(platform("com.google.firebase:firebase-bom:31.3.0"))
    // Declare the dependencies for the Crashlytics and Analytics libraries
    // When using the BoM, you don"t specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.10")
    implementation("com.github.franmontiel:PersistentCookieJar:v1.0.1")
    implementation("hu.autsoft:krate:2.0.0")
    implementation("hu.autsoft:krate-kotlinx:2.0.0")
    implementation("com.frybits.harmony:harmony:1.2.2")
    implementation("io.github.kawaiidango.kumo-android:kumo-core:1.28.1")
    implementation("io.michaelrocks.bimap:bimap:1.1.0")
    implementation("com.github.hadilq:live-event:1.3.0")
    implementation("com.ernestoyaquello.stepperform:vertical-stepper-form:2.7.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.telefonica:nestedscrollwebview:0.1.1")
    val coilVersion = "2.3.0"
    implementation("io.coil-kt:coil:$coilVersion")
    implementation("io.coil-kt:coil-gif:$coilVersion")

    val ktorVersion = "2.2.4"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    implementation("com.mikepenz:aboutlibraries-core:10.6.1")
//    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")

    implementation(project(":lastfm"))

    testImplementation("junit:junit:4.13.2")
    // androidTestImplementation("androidx.test:runner:1.1.0-alpha1")
    // androidTestImplementation("androidx.test.espresso:espresso-core:3.1.0-alpha1")
}


// play store and github publishing scripts
// remove if not needed

val localProperties = gradleLocalProperties(rootDir) as Map<String, String>

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
    repo("pscrobbler")
    val changelog = file("src/main/play/release-notes/en-US/default.txt").readText() +
            "\n\n" + "Copied from Play Store what's new, may not be accurate for minor updates."
    body(changelog)
    tagName(android.defaultConfig.versionCode.toString())
    releaseName(android.defaultConfig.versionName)
    targetCommitish("main")
    releaseAssets(
        listOf(
            "build/outputs/apk/release/pScrobbler-release.apk",
        )
    )
    draft(false) // by default this is true
    allowUploadToExisting(false) // Setting this to true will allow this plugin to upload artifacts to a release if it found an existing one. If overwrite is set to true, this option is ignored.
    overwrite(false) // by default false; if set to true, will delete an existing release with the same tag and name
    dryRun(false) // by default false; you can use this to see what actions would be taken without making a release
}