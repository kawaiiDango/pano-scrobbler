import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import com.google.gson.Gson
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.StrictMode
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.ComposeHotRun
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileFilter
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
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
    alias(libs.plugins.hot.reload)
    id("kotlin-parcelize")
}

val os = org.gradle.internal.os.OperatingSystem.current()
val arch = System.getProperty("os.arch")

val archAmd64 = arrayOf("amd64", "x86_64")
val archArm64 = arrayOf("aarch64", "arm64")

val resourcesDirName = when {
    os.isMacOsX && arch in archAmd64 -> "macos-x64"
    os.isMacOsX && arch in archArm64 -> "macos-arm64"
    os.isLinux && arch in archAmd64 -> "linux-x64"
    os.isLinux && arch in archArm64 -> "linux-arm64"
    os.isWindows && arch in archAmd64 -> "windows-x64"
    os.isWindows && arch in archArm64 -> "windows-arm64"
    else -> throw IllegalStateException("Unsupported platform: $os $arch")
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

composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}

kotlin {
    androidTarget {
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
            implementation(libs.coil.gif)

//            implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("acrcloud*.jar"))))

        }
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlin.coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
//            implementation(compose.material3)
            implementation(libs.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.uiUtil)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime)
            implementation(libs.coil.compose)
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
            implementation(projects.extrasNonplay)
            implementation(libs.androidx.sqlite.bundled)

            val platform = when {
                os.isWindows -> "win"
                os.isMacOsX -> "mac"
                else -> "linux"
            }
            val javafxVersion = libs.versions.javafx.get()

            implementation("org.openjfx:javafx-base:$javafxVersion:$platform")
            implementation("org.openjfx:javafx-swing:$javafxVersion:$platform")
            implementation("org.openjfx:javafx-graphics:$javafxVersion:$platform")
            implementation("org.openjfx:javafx-controls:$javafxVersion:$platform")
            implementation("org.openjfx:javafx-media:$javafxVersion:$platform")
            implementation("org.openjfx:javafx-web:$javafxVersion:$platform")
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
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

    val isReleaseBuild = gradle.startParameter.taskNames.any {
        it.contains("proguard", ignoreCase = true) || it.contains("release", ignoreCase = true)
    }

    // default config is required
    defaultConfigs {
        buildConfigField(STRING, "APP_NAME", appName, const = true)
        buildConfigField(STRING, "APP_ID", appId, const = true)
        buildConfigField(INT, "VER_CODE", verCode.toString(), const = true)
        buildConfigField(STRING, "VER_NAME", verNameWithDate, const = true)
        buildConfigField(STRING, "CHANGELOG", changelog, const = true)
        buildConfigField(BOOLEAN, "DEBUG", (!isReleaseBuild).toString(), const = true)
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
//    compileSdkPreview = "Baklava"

    defaultConfig {
        applicationId = appId
        namespace = appId
        minSdk = libs.versions.minSdk.get().toInt()
//        targetSdkPreview = "Baklava"
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

        releaseImplementation(projects.extrasPlay)
//    debugImplementation(projects.extrasPlay)
        debugImplementation(projects.extrasNonplay)
        "releaseGithubImplementation"(projects.extrasNonplay)

        androidTestImplementation(libs.androidx.uiautomator)
        androidTestImplementation(libs.androidx.runner)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(libs.androidx.test.ext.junit)
    }
}

aboutLibraries {
    offlineMode = true
    collect {
        configPath = File("aboutLibsConfig")
        fetchRemoteLicense = false
        fetchRemoteFunding = false
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
            exportVariant = "release"
        }
        license.strictMode = StrictMode.FAIL
        library.duplicationMode = DuplicateMode.MERGE
        android.registerAndroidTasks = false
    }
}

compose.desktop {
    application {
        mainClass = "com.arn.scrobble.main.MainKt"
        // ZGC starts with ~70MB minimized and goes down to ~160MB after re-minimizing
        jvmArgs += listOf(
            "-Djava.library.path=\$APPDIR/resources${pathSeperator}./resources/$resourcesDirName",
            "-Ddev.resources.dir.name=$resourcesDirName",
            // use sw rendering on linux instead of es2, fixes the no text on webview bug on arch
            "-Dprism.order=sw",
//            "-XX:NativeMemoryTracking=detail",
            "-XX:+UseSerialGC",
            "-XX:+UseAdaptiveSizePolicy",
//            "-XX:+UseZGC",
//            "-XX:ZUncommitDelay=60",
            "-XX:+UseStringDeduplication",
            "-Xms32m",
            "-Xmx512m",
        )

        nativeDistributions {
            val formats = when {
                os.isWindows -> {
                    mutableSetOf(TargetFormat.Msi, TargetFormat.AppImage)
                }

                os.isLinux -> {
                    mutableSetOf(TargetFormat.AppImage)
                }

                os.isMacOsX -> {
                    mutableSetOf(TargetFormat.Dmg)
                }

                else -> {
                    throw IllegalStateException("Unsupported OS: $os")
                }
            }

            targetFormats = formats
            packageVersion = "$verName.0"
            vendor = "kawaiiDango"

            appResourcesRootDir = project.layout.projectDirectory.dir("resources")

//            includeAllModules = true
            modules(
                "java.net.http",
                "jdk.jsobject",
                "jdk.unsupported",
                "jdk.unsupported.desktop",
                "jdk.xml.dom"
            )

            windows {
                dirChooser = true
                upgradeUuid = "85173f4e-ca52-4ec9-b77f-c2e0b1ff4209"
                msiPackageVersion = packageVersion
                exePackageVersion = packageVersion
                packageName = appName
                menuGroup = appName
                description = appName
                iconFile = project.layout.projectDirectory.dir("resources")
                    .file("windows/app_icon.ico")
            }

            linux {
                packageName = appNameWithoutSpaces
                menuGroup = appNameWithoutSpaces
                description = appNameWithoutSpaces
                iconFile = project.layout.projectDirectory.dir("resources")
                    .file("linux/app_icon.png")
            }

            macOS {
                bundleID = appId
                packageName = appName
                dockName = appName
                description = appName
                appStore = false

                iconFile = project.layout.projectDirectory.dir("resources")
                    .file("macos/app_icon.icns")
            }
        }

        buildTypes.release {
            proguard {
                obfuscate = true
                optimize = false
                joinOutputJars = true
                version = "7.7.0"
                configurationFiles.from(project.file("proguard-rules-desktop.pro"))
            }
        }
    }
}

tasks.register<ComposeHotRun>("runHot") {
    mainClass = "com.arn.scrobble.main.MainKt"
    jvmArgs = (jvmArgs ?: emptyList()) + listOf(
        "-Djava.library.path=\$APPDIR/resources${pathSeperator}./resources/$resourcesDirName",
        "-Ddev.resources.dir.name=$resourcesDirName",
    )
    val appDataRoot = when {
        os.isWindows -> {
            System.getenv("APPDATA")?.ifEmpty { null }
                ?: System.getProperty("user.home")
        }

        os.isLinux -> {
            System.getenv("XDG_DATA_HOME")?.ifEmpty { null }
                ?: (System.getProperty("user.home") + "/.local/share")
        }

        os.isMacOsX -> {
            System.getProperty("user.home") + "/Library/Application Support"
        }

        else -> throw IllegalStateException("unsupported os")
    }

    val appDataDir = File(appDataRoot, "$appNameWithoutSpaces-debug").absolutePath
    args = listOf("--data-dir=$appDataDir")
}

tasks.register<DefaultTask>("generateSha256") {
    val distFile = file("dist")

    doLast {
        distFile.listFiles { file ->
            file.isFile && !file.name.endsWith(".sha256")
        }?.forEach { file ->
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            file.let { File(it.parent, "${it.name}.sha256") }
                .writeText("$hash  ${file.name}")
            println("SHA-256 checksum file created: ${file.name}.sha256")
        }
    }
}

tasks.register<Zip>("zipAppImage") {
    from("build/compose/binaries/main-release/app")
    archiveFileName = "$appNameWithoutSpaces-$verCode-$resourcesDirName.zip"
    destinationDirectory = file("dist")
}

tasks.register<Copy>("copyGithubReleaseApk") {
    from("build/outputs/apk/releaseGithub")
    into("dist")
    include("*-releaseGithub.apk")
    rename(
        "(.*)-releaseGithub.apk",
        "$appNameWithoutSpaces-$verCode-android-universal.apk"
    )
}

tasks.register<Copy>("copyReleaseMsi") {
    val fileName = "$appNameWithoutSpaces-$verCode-$resourcesDirName.msi"

    from("build/compose/binaries/main-release/msi")
    into("dist")
    include("*.msi")
    rename(
        "(.*).msi",
        fileName
    )
}

tasks.register<Copy>("copyReleaseDmg") {
    val fileName = "$appNameWithoutSpaces-$verCode-$resourcesDirName.dmg"
    from("build/compose/binaries/main-release/dmg")
    into("dist")
    include("*.dmg")
    rename(
        "(.*).dmg",
        fileName
    )
}

tasks.register<Exec>("packageLinuxAppImage") {

    val executableDir = file("build/compose/binaries/main-release/app/Pano Scrobbler")
    val appDir = file("build/compose/binaries/main-release/app/PanoScrobbler.AppDir")
    val appimageFilesDir = file("appimage-files")
    val distDir = file("dist")

    doFirst {
        // Create the AppDir structure. Copy all your distribution files.
        appDir.deleteRecursively()
        appDir.mkdirs()
        File(appDir, "usr").mkdirs()

        // Copy the distribution files to the AppDir.
        executableDir.copyRecursively(File("$appDir/usr"))

        // Copy the PNG icon from the resources to the AppDir root.
        val iconSource = File("$appDir/usr/lib/app/resources/app_icon.png")
        val iconDest = File("$appDir/pano-scrobbler.png")
        if (iconSource.exists()) {
            iconSource.copyTo(iconDest, overwrite = true)
        } else {
            throw GradleException("Icon not found at ${iconSource.absolutePath}")
        }

        // Copy the desktop file and AppRun script to the AppDir.

        appimageFilesDir
            .listFiles(FileFilter { it.isFile && !it.name.lowercase().endsWith(".appimage") })
            ?.forEach {
                val destFile = File(appDir, it.name)
                it.copyTo(destFile, overwrite = true)
            }
    }

    val apppImageToolFile = File(appimageFilesDir, "appimagetool-x86_64.AppImage")

    if (!apppImageToolFile.exists()) {
        commandLine(
            "wget",
            "-O",
            apppImageToolFile.absolutePath,
            "https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage",
        )
    }

    val distFile = File(distDir, "$appNameWithoutSpaces-$verCode-$resourcesDirName.AppImage")
    commandLine(
        apppImageToolFile.absolutePath,
        appDir.absolutePath,
        distFile.absolutePath
    )
}

afterEvaluate {
    if (os.isWindows)
        tasks.named("packageReleaseAppImage") {
            finalizedBy(tasks.named("zipAppImage"))
        }

    if (os.isLinux)
        tasks.named("packageReleaseAppImage") {
            finalizedBy(tasks.named("packageLinuxAppImage"))
        }

    if (os.isWindows)
        tasks.named("packageReleaseMsi") {
            finalizedBy(tasks.named("copyReleaseMsi"))
        }

    if (os.isMacOsX)
        tasks.named("packageReleaseDmg") {
            finalizedBy(tasks.named("copyReleaseDmg"))
        }

    tasks.named("packageReleaseGithub") {
        finalizedBy(tasks.named("copyGithubReleaseApk"))
    }

    tasks.named("githubRelease") {
        dependsOn(tasks.named("generateSha256"))
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
    val localeUtilsFile = file("src/commonMain/kotlin/com/arn/scrobble/utils/LocaleUtils.kt")

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
        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
        }

        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }

        getByName("releaseGithub") {
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
            ""
//            "\n\n" + "Copied from Play Store what's new, may not be accurate for minor updates."
    body = changelog
    tagName = verCode.toString()
    releaseName = verNameWithDate
    targetCommitish = "main"

    val assets = file("dist")
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