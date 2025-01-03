import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import com.google.gson.Gson
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.StrictMode
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.github.release)
    alias(libs.plugins.play.publisher)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.baselineprofile)
    id("kotlin-parcelize")
}

val os = org.gradle.internal.os.OperatingSystem.current()

val platform = when {
    os.isWindows -> "win"
    os.isMacOsX -> "mac"
    else -> "linux"
}
val pathSeperator = File.pathSeparator!!

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
val verName = "${verCode / 100}.${verCode % 100}"
val verNameWithDate =
    "$verName - ${SimpleDateFormat("YYYY, MMM dd").format(Date())}"
val appId = "com.arn.scrobble"
val appName = "Pano Scrobbler"
val appNameWithoutSpaces = "pano-scrobbler"

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(libs.core)
            implementation(libs.appcompat)

            implementation(compose.preview)
            implementation(libs.activity.compose)
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.core.remoteviews)
            implementation(libs.media)
            implementation(libs.compose.webview)
            implementation(libs.documentfile)
            implementation(libs.harmony)

            implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("acrcloud*.jar"))))

        }
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlin.coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.uiUtil)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime)
            implementation(libs.coil.compose)
            implementation(libs.coil.gif)
            implementation(libs.coil.network.okhttp)
            implementation(project.dependencies.platform(libs.ktor.bom))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.okhttp)
            implementation(libs.aboutlibraries.core)
            implementation(libs.kotlin.csv.jvm)
            implementation(libs.kermit)
            implementation(libs.kermit.io)
            implementation(libs.compose.shimmer)
            implementation(libs.datastore.core)
            implementation(libs.paging.common)
            implementation(projects.androidxMod.androidx.paging.compose)
            implementation(libs.navigation.compose)
            implementation(libs.compose.adaptive)
            implementation(libs.koalaplot.core)
            implementation(libs.nanohttpd)
            implementation(libs.androidx.room.runtime)
            implementation(libs.kotlinx.immutable)
            implementation(projects.extrasCommon)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(projects.extrasNonPlay)
            implementation(libs.androidx.sqlite.bundled)

            val javafxVersion = libs.versions.javafx.get()

            implementation("org.openjfx:javafx-base:$javafxVersion:$platform")
            implementation("org.openjfx:javafx-swing:$javafxVersion:$platform")
            implementation("org.openjfx:javafx-graphics:$javafxVersion:$platform")
            implementation("org.openjfx:javafx-controls:$javafxVersion:$platform")
            implementation("org.openjfx:javafx-media:$javafxVersion:$platform")
            implementation("org.openjfx:javafx-web:$javafxVersion:$platform")
        }
    }
}


dependencies {
    "baselineProfile"(projects.baselineprofile)
    implementation(libs.profileinstaller)
    debugImplementation(compose.uiTooling)

    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
    generateKotlin = true
}

buildkonfig {
    packageName = appId

    val changelogFile = file("src/androidMain/play/release-notes/en-US/default.txt")
    val changelog = if (changelogFile.canRead()) {
        changelogFile.readText()
    } else {
        "changelog_placeholder"
    }

    // default config is required
    defaultConfigs {
        buildConfigField(STRING, "APP_NAME", appName, const = true)
        buildConfigField(STRING, "APP_ID", appId, const = true)
        buildConfigField(INT, "VER_CODE", verCode.toString(), const = true)
        buildConfigField(STRING, "VER_NAME", verNameWithDate, const = true)
        buildConfigField(STRING, "CHANGELOG", changelog, const = true)
    }

    targetConfigs {
        // names in create should be the same as target names you specified
        create("android") {
//            buildConfigField(STRING, "nullableField", "NonNull-value", nullable = true)
        }

        create("desktop") {
//            buildConfigField(STRING, "name", "valueForNative")
        }
    }
}


android {
    compileSdk = libs.versions.targetSdk.get().toInt()

    defaultConfig {
        applicationId = appId
        namespace = appId
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = verCode
        versionName = verNameWithDate
        setProperty("archivesBaseName", appNameWithoutSpaces)

    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
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
//            extra["enableCrashlytics"] = false
        }

        create("releaseGithub") {
            initWith(getByName("release"))
            versionNameSuffix = " GH"
        }
    }

    packaging {
        resources {
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    dependencies {
//        implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("acrcloud*.jar"))))

//        "baselineProfile"(project(mapOf("path" to ":baselineprofile")))

        coreLibraryDesugaring(libs.desugar.jdk.libs)
        releaseImplementation(projects.extrasPlay)
//    debugImplementation(projects.extrasPlay)
        debugImplementation(projects.extrasNonPlay)
        "releaseGithubImplementation"(projects.extrasNonPlay)

        androidTestImplementation(libs.androidx.uiautomator)
        androidTestImplementation(libs.androidx.runner)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(libs.androidx.test.ext.junit)
    }
//    kotlinOptions {
//        freeCompilerArgs += "-Xjvm-default=all"
//        jvmTarget = "21"
//    }
//
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
    registerAndroidTasks = false
}

compose.desktop {
    application {
        mainClass = "com.arn.scrobble.main.MainKt"
        // ZGC starts with ~70MB minimized and goes down to ~160MB after re-minimizing
        jvmArgs += listOf(
            "-Djava.library.path=\$APPDIR/resources${pathSeperator}./resources/${os.familyName}",
//            "-XX:NativeMemoryTracking=detail",
//            "-XX:+UseSerialGC",
//            "-XX:+UseAdaptiveSizePolicy",
            "-XX:+UseZGC",
            "-XX:+ZGenerational",
            "-XX:ZUncommitDelay=60",
            "-XX:+UseStringDeduplication",
            "-Xms32m",
            "-Xmx512m",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.AppImage)
            packageVersion = "$verName.0"
            description = appName
            vendor = "kawaiiDango"

            appResourcesRootDir = project.layout.projectDirectory.dir("resources")

//            modules("javafx.controls","javafx.media")
            includeAllModules = true

            windows {
                console = true
                dirChooser = true
//                perUserInstall = true
                upgradeUuid = "85173f4e-ca52-4ec9-b77f-c2e0b1ff4209"
                msiPackageVersion = packageVersion
                exePackageVersion = packageVersion
                packageName = appName
                menuGroup = appName
                iconFile = project.layout.projectDirectory.dir("resources")
                    .file("windows/app_icon.ico")
            }

            linux {
                packageName = appNameWithoutSpaces
                menuGroup = appNameWithoutSpaces
                iconFile = project.layout.projectDirectory.dir("resources")
                    .file("linux/app_icon.png")
            }
        }

        buildTypes.release {
            proguard {
                obfuscate = false
                optimize = false
                joinOutputJars = true
                version = "7.5.0"
                configurationFiles.from(project.file("proguard-rules-desktop.pro"))
            }
        }
    }
}

tasks.register<Zip>("zipAppImage") {
    from("build/compose/binaries/main-release/app")
    archiveFileName = "$appNameWithoutSpaces-$verCode-${os.familyName}-release.zip"
    destinationDirectory = file("release-builds")
}

tasks.register<Copy>("copyGithubReleaseApk") {
    from("build/outputs/apk/releaseGithub")
    into("release-builds")
    include("*-releaseGithub.apk")
    rename(
        "(.*)-releaseGithub.apk",
        "$appNameWithoutSpaces-$verCode-android-release.apk"
    )
}

tasks.register<Copy>("copyReleaseMsi") {
    from("build/compose/binaries/main-release/msi")
    into("release-builds")
    include("*.msi")
    rename(
        "(.*).msi",
        "$appNameWithoutSpaces-$verCode-${os.familyName}-release.msi"
    )
}

afterEvaluate {
    tasks.named("packageReleaseAppImage") {
        finalizedBy(tasks.named("zipAppImage"))
    }

    tasks.named("packageReleaseMsi") {
        finalizedBy(tasks.named("copyReleaseMsi"))
    }

    tasks.named("packageReleaseGithub") {
        finalizedBy(tasks.named("copyGithubReleaseApk"))
    }
}

data class CrowdinMember(val username: String)
data class CrowdinMemberData(val data: CrowdinMember)
data class CrowdinMembersRoot(val data: List<CrowdinMemberData>)

tasks.register("fetchCrowdinMembers") {
    val projectIdProvider = project.provider { localProperties["crowdin.project"]!! }
    val tokenProvider = project.provider { localProperties["crowdin.token"]!! }
    val membersFile = file("src/commonMain/composeResources/files/crowdin_members.txt")
    outputs.file(membersFile)

    doLast {
        val projectId = projectIdProvider.get()
        val token = tokenProvider.get()

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
            val root = gson.fromJson(responseJson, CrowdinMembersRoot::class.java)
            val userDataList = root.data

            val outputLines = userDataList.joinToString("\n") { it.data.username }

            membersFile.writeText(outputLines)
            println("Crowdin members fetched successfully.")
        } else {
            throw IOException("Failed to fetch Crowdin members. Response code: $responseCode")
        }
    }
}

data class CrowdinLanguageProps(val twoLettersCode: String)
data class CrowdinLanguage(
    val languageId: String,
    val language: CrowdinLanguageProps,
    val translationProgress: Int,
)

data class CrowdinLanguageData(val data: CrowdinLanguage)
data class CrowdinLanguagesRoot(val data: List<CrowdinLanguageData>)

tasks.register("fetchCrowdinLanguages") {
    val projectIdProvider = project.provider { localProperties["crowdin.project"]!! }
    val tokenProvider = project.provider { localProperties["crowdin.token"]!! }
    val localesConfigFile = file("src/androidMain/res/xml/locales_config.xml")
    val localeUtilsFile = file("src/androidMain/kotlin/com/arn/scrobble/utils/LocaleUtils.kt")

    outputs.file(localesConfigFile)
    outputs.file(localeUtilsFile)

    doLast {
        val projectId = projectIdProvider.get()
        val token = tokenProvider.get()
        val minProgress = 5
        val customMappings = mapOf(
            "zh-CN" to "zh-Hans",
            "pt-BR" to "pt-BR",
        )

        val url =
            URL("https://api.crowdin.com/api/v2/projects/$projectId/languages/progress?limit=500")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 3000

        val responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseJson = conn.inputStream.bufferedReader().readText()

            val gson = Gson()
            val root = gson.fromJson(responseJson, CrowdinLanguagesRoot::class.java)
            val userDataList = root.data

            val languagesFiltered = (
                    userDataList.filter {
                        it.data.translationProgress >= minProgress
                    }.map {
                        customMappings[it.data.languageId] ?: it.data.language.twoLettersCode
                    } + "en"
                    ).sorted()

            // write to locale_config.xml
            val localesConfigText =
                """<?xml version='1.0' encoding='UTF-8'?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
${languagesFiltered.joinToString("\n") { "    <locale android:name=\"$it\" />" }}
</locale-config>
"""
            localesConfigFile.writeText(localesConfigText)

            // write to LocaleUtils.kt
            val localeUtilsPartialText = """
    val localesSet = arrayOf(
${languagesFiltered.joinToString("\n") { "        \"$it\"," }}
    )
"""

            val localeUtilsText = localeUtilsFile.readText()
            val start =
                localeUtilsText.indexOf("// localesSet start") + "// localesSet start".length
            val end = localeUtilsText.indexOf("    // localesSet end")
            val newLocaleUtilsText = localeUtilsText.substring(
                0,
                start
            ) + localeUtilsPartialText + localeUtilsText.substring(end)
            localeUtilsFile.writeText(newLocaleUtilsText)

            println("Crowdin languages fetched successfully.")
        } else {
            throw IOException("Failed to fetch Crowdin languages. Response code: $responseCode")
        }
    }
}


// workaround for java.lang.IllegalStateException: Android context is not initialized. If it happens in the Preview mode then call PreviewContextConfigurationEffect() function.
tasks.register("copyStringsToAndroid") {
    val sourceDirs = file("src/commonMain/composeResources").listFiles { file ->
        file.isDirectory && file.name.startsWith("values")
    } ?: return@register

    val targetDirs = sourceDirs.map { sourceDir ->
        val targetDirName = sourceDir.name
        file("src/androidMain/res/$targetDirName")
    }

    val stringsListFile = file("strings-to-copy-to-android.txt")

    doLast {
        val stringsToCopyToAndroid = stringsListFile.readLines().toSet()

        sourceDirs.zip(targetDirs).forEach { (sourceDir, targetDir) ->
            targetDir.mkdirs()
            val sourceFile = File(sourceDir, "strings.xml")
            val targetFile = File(targetDir, "strings-android.xml")

            if (sourceFile.exists()) {
                val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                val sourceDoc = docBuilder.parse(sourceFile)
                val targetDoc = docBuilder.newDocument()

                val resourcesElement = targetDoc.createElement("resources")
                resourcesElement.setAttribute("xmlns:tools", "http://schemas.android.com/tools")
                resourcesElement.setAttribute("tools:ignore", "MissingTranslation")
                targetDoc.appendChild(resourcesElement)

                val sourceStrings = sourceDoc.getElementsByTagName("string")
                for (i in 0 until sourceStrings.length) {
                    val stringElement = sourceStrings.item(i)
                    if (stringElement is org.w3c.dom.Element) {
                        val name = stringElement.getAttribute("name")
                        if (name in stringsToCopyToAndroid) {
                            val importedNode = targetDoc.importNode(stringElement, true)
                            resourcesElement.appendChild(importedNode)
                        }
                    }
                }

                val sourcePlurals = sourceDoc.getElementsByTagName("plurals")
                for (i in 0 until sourcePlurals.length) {
                    val pluralElement = sourcePlurals.item(i)
                    if (pluralElement is org.w3c.dom.Element) {
                        val name = pluralElement.getAttribute("name")
                        if (name in stringsToCopyToAndroid) {
                            val importedNode = targetDoc.importNode(pluralElement, true)
                            resourcesElement.appendChild(importedNode)
                        }
                    }
                }

                val transformer = TransformerFactory.newInstance().newTransformer()
                transformer.setOutputProperty(OutputKeys.INDENT, "yes")
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
                transformer.transform(DOMSource(targetDoc), StreamResult(targetFile))
            }
        }
    }
}


// play store and github publishing scripts ===================================================
// remove if not needed

val localProperties = gradleLocalProperties(rootDir, project.providers)
    .map { it.key to it.value.toString() }
    .toMap()

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
        defaultConfig {
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
    owner = "kawaiidango"
    repo = "pano-scrobbler"
    val changelog = file("src/androidMain/play/release-notes/en-US/default.txt").readText() +
            "\n\n" + "Copied from Play Store what's new, may not be accurate for minor updates."
    body = changelog
    tagName = verCode.toString()
    releaseName = verNameWithDate
    targetCommitish = "main"

    val assets = file("release-builds")
        .listFiles { file -> file.isFile }
        ?.map { it.absolutePath }
        ?.toList() ?: emptyList()
    releaseAssets(assets)

    // by default this is true
    draft = false
    // Setting this to true will allow this plugin to upload artifacts to a release if it found an existing one. If overwrite is set to true, this option is ignored.
    allowUploadToExisting = false
    // by default false; if set to true, will delete an existing release with the same tag and name
    overwrite = false
    // by default false; you can use this to see what actions would be taken without making a release
    dryRun = true
}