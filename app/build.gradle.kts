
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.StrictMode
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-parcelize")
    id ("com.github.triplet.play")
    kotlin("plugin.serialization") version "1.7.10"
    id("com.mikepenz.aboutlibraries.plugin") version "10.5.1"
    id("com.github.breadmoirai.github-release") version "2.4.1"
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
    defaultConfig {
        applicationId = "com.arn.scrobble"
        namespace = "com.arn.scrobble"
        minSdk = 21
        targetSdk = 33
        versionCode = verCode
        versionName = "${verCode / 100}.${verCode % 100} - ${
            SimpleDateFormat("YYYY, MMM dd").format(Date())
        }"
        setProperty("archivesBaseName", "pScrobbler")
        vectorDrawables.useSupportLibrary = true
//        resConfigs "af", "am", "ar", "as", "az", "be", "bg", "bn", "bs", "ca", "cs", "da", "de", "el", "en", "es", "et", "eu", "fa", "fi", "fil", "fr", "gl", "gsw", "gu", "he", "hi", "hr", "hu", "hy", "id(", ")is", "it", "iw", "ja", "ka", "kk", "km", "kn", "ko", "ky", "ln", "lo", "lt", "lv", "mk", "ml", "mn", "mo", "mr", "ms", "my", "nb", "ne", "nl", "no", "or", "pa", "pl", "pt", "ro", "ru", "si", "sk", "sl", "sq", "sr", "sv", "sw", "ta", "te", "th", "tl", "tr", "uk", "ur", "uz", "vi", "zh", "zh-rTW", "zu"
        //this removes regional variants from the support libraries
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }

    }
    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"))
        }

        debug {
            versionNameSuffix = " DEBUG"
//            applicationIdSuffix = ".debug"
//            extra["enableCrashlytics"] = false
        }
    }

    lint {
        disable += "UseRequireInsteadOfGet"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        // dont use yet, creates weird errors
        // useFir = true
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xjvm-default=all"
    }
}

aboutLibraries {
    configPath = "aboutLibsConfig"
    offlineMode = true
    fetchRemoteLicense = false
    fetchRemoteFunding = false
    excludeFields = arrayOf("developers", "funding", "description", "organization", "content", "connection", "developerConnection")
    strictMode = StrictMode.FAIL
    duplicationMode = DuplicateMode.MERGE
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.appcompat:appcompat:1.7.0-alpha01")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.media:media:1.6.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.2")
    kapt("androidx.room:room-compiler:2.4.3")
    implementation("androidx.room:room-runtime:2.4.3")
    implementation("com.android.billingclient:billing:5.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    // viewpager2 doesnt respond to left/right press on TVs, don"t migrate

    implementation("com.google.android.material:material:1.8.0-alpha01")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation(platform("com.google.firebase:firebase-bom:31.0.0"))
    // Declare the dependencies for the Crashlytics and Analytics libraries
    // When using the BoM, you don"t specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    val coilVersion = "2.2.2"
    implementation("io.coil-kt:coil:$coilVersion")
    implementation("io.coil-kt:coil-gif:$coilVersion")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.10")
    implementation("com.github.franmontiel:PersistentCookieJar:v1.0.1")
    implementation("hu.autsoft:krate:2.0.0")
    implementation("hu.autsoft:krate-kotlinx:2.0.0")
    implementation("com.frybits.harmony:harmony:1.2.1")
    implementation("io.github.kawaiidango.kumo-android:kumo-core:1.28.1")
    implementation("io.michaelrocks.bimap:bimap:1.1.0")
    implementation("com.github.hadilq:live-event:1.3.0")
    implementation("com.ernestoyaquello.stepperform:vertical-stepper-form:2.7.0")

    val ktorVersion = "2.1.2"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

//    implementation("com.brandongogetap:stickyheaders:0.6.2")
    implementation("com.mikepenz:aboutlibraries-core:10.5.1")
    //    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.7")

    implementation(project(":lastfm"))
    implementation(project(":sparkline"))

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