import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.google.gson.Gson
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.StrictMode
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Random

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

    compileSdk = libs.versions.targetSdk.get().toInt()
    defaultConfig {
        applicationId = "com.arn.scrobble"
        namespace = "com.arn.scrobble"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
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

        val changelogFile = file("src/main/play/release-notes/en-US/default.txt")
        if (changelogFile.canRead()) {
            val changelog = changelogFile.readText()
            resValue("string", "changelog_text", "\"$changelog\"")
        } else {
            resValue("string", "changelog_text", "changelog_placeholder")
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

    implementation(libs.profileinstaller)
    "baselineProfile"(project(mapOf("path" to ":baselineprofile")))

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.kotlin.stdlib)
    implementation(libs.appcompat)
    implementation(libs.core)
    implementation(libs.fragment)
    implementation(libs.preference.ktx)
    implementation(libs.media)
    implementation(libs.palette.ktx)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.core.remoteviews)
    implementation(libs.androidx.transition)
    implementation(libs.androidx.activity)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.billing)
    implementation(libs.review)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.android.snowfall)
    implementation(libs.kotlin.csv.jvm)
    implementation(libs.androidx.viewpager)
    // viewpager2 doesnt respond to left/right press on TVs, don"t migrate

    implementation(libs.material)
    implementation(libs.timber)
    implementation(platform(libs.firebase.bom))
    // Declare the dependencies for the Crashlytics and Analytics libraries
    // When using the BoM, you don"t specify versions in Firebase library dependencies
    implementation(libs.crashlytics)

    implementation(libs.okhttp)
    implementation(libs.krate)
    implementation(libs.krate.kotlinx)
    implementation(libs.harmony)
    implementation(libs.kumo.core)
    implementation(libs.mpAndroidChart)
    implementation(libs.nestedscrollwebview)
    implementation(libs.skeletonlayout)
    implementation(libs.coil)
    implementation(libs.coil.gif)
    implementation(libs.coil.network.okhttp)

    implementation(platform(libs.ktor.bom))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.nanohttpd)

    implementation(libs.aboutlibraries.core)
//    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.espresso.core)
}

// https://yrom.net/blog/2019/06/19/simple-codes-to-generate-obfuscation-dictionary/

tasks.register("genDict") {
    val dictFile = file("build/tmp/dict.txt")
    outputs.file(dictFile)
    doLast {
        val r = Random()
//        val start = r.nextInt(1000) + 0x0100
//        val end = start + 0x4000
        val start = 'A'.code
        val end = 'z'.code
        val chars = (start..end)
            .filter { Character.isValidCodePoint(it) && Character.isJavaIdentifierPart(it) }
            .map { it.toChar().toString() }
            .toMutableList()
        val max = chars.size
        val startChars = mutableListOf<String>()
        val dict = mutableListOf<String>()

        for (i in 0 until max) {
            val c = chars[i][0]
            if (Character.isJavaIdentifierStart(c)) {
                startChars.add(c.toString())
            }
        }
        val startSize = startChars.size

        chars.shuffle(r)
        startChars.shuffle(r)

        for (i in 0 until max) {
            val m = r.nextInt(startSize - 2)
            val n = m + 2
            for (j in m..n) {
                dict.add(startChars[j] + chars[i])
            }
        }

        dictFile.parentFile.mkdirs()
        dictFile.writeText(
            startChars.joinToString(System.lineSeparator()) + dict.joinToString(
                System.lineSeparator()
            )
        )
    }
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

fun fetchCrowdinMembers(projectId: String, token: String) {
    data class Data(val username: String)
    data class UserData(val data: Data)
    data class Root(val data: List<UserData>)

    val url =
        URL("https://api.crowdin.com/api/v2/projects/$projectId/members?limit=500&orderBy=username&role=translator")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.setRequestProperty("Authorization", "Bearer $token")
    conn.setRequestProperty("Accept", "application/json")
    conn.connectTimeout = 3000

    val responseCode = conn.responseCode
    if (responseCode == HttpURLConnection.HTTP_OK) {
        val responseJson = conn.inputStream.bufferedReader().readText()

        val gson = Gson()
        val root = gson.fromJson(responseJson, Root::class.java)
        val userDataList = root.data

        val outputLines = userDataList.joinToString("\n") { it.data.username }

        file("src/main/res/raw/crowdin_members.txt").writeText(outputLines)
        println("Crowdin members fetched successfully.")
    } else {
        throw IOException("Failed to fetch Crowdin members. Response code: $responseCode")
    }
}

tasks.register("fetchCrowdinMembers") {
    doLast {
        fetchCrowdinMembers(
            localProperties["crowdin.project"]!!,
            localProperties["crowdin.token"]!!
        )
    }
}

