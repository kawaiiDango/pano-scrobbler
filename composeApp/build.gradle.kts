import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import com.google.gson.Gson
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.StrictMode
import org.gradle.kotlin.dsl.get
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.gradle.ComposeHotRun
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.io.path.name
import kotlin.io.use
import kotlin.io.walk
import kotlin.sequences.filter
import kotlin.sequences.forEach

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildkonfig)
    id("kotlin-parcelize")
}

val os = org.gradle.internal.os.OperatingSystem.current()!!
val arch = System.getProperty("os.arch")!!

val archAmd64 = arrayOf("amd64", "x86_64")
val archArm64 = arrayOf("aarch64", "arm64")

val isReleaseBuild = gradle.startParameter.taskNames.any {
    it.contains("proguard", ignoreCase = true) || it.contains("release", ignoreCase = true) ||
            it.contains("packageUberJarForCurrentOS", ignoreCase = true)
}
val resourcesDirName = when {
    os.isMacOsX && arch in archAmd64 -> "macos-x64"
    os.isMacOsX && arch in archArm64 -> "macos-arm64"
    os.isLinux && arch in archAmd64 -> "linux-x64"
    os.isLinux && arch in archArm64 -> "linux-arm64"
    os.isWindows && arch in archAmd64 -> "windows-x64"
    os.isWindows && arch in archArm64 -> "windows-arm64"
    else -> throw IllegalStateException("Unsupported platform: $os $arch")
}

val APP_ID: String by rootProject.extra
val VER_CODE: Int by rootProject.extra
val VER_NAME: String by rootProject.extra
val BUILD_DATE: String by rootProject.extra
val APP_NAME: String by rootProject.extra
val APP_NAME_NO_SPACES: String by rootProject.extra
val CHANGELOG: String by rootProject.extra

val localProperties = gradleLocalProperties(rootDir, project.providers)
    .map { it.key to it.value.toString() }
    .toMap()

kotlin {
    android {
        compileSdk = libs.versions.targetSdk.get().toInt()
        namespace = APP_ID
        minSdk = libs.versions.minSdk.get().toInt()

        androidResources {
            enable = true
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(libs.core)
            implementation(libs.appcompat)

            implementation(libs.activity.compose)
            implementation(libs.androidx.work.runtime)
            implementation(libs.androidx.core.remoteviews)
            implementation(libs.media)
            implementation(libs.compose.webview)
            implementation(libs.harmony)
            implementation(libs.coil.gif)
            implementation(projects.extrasDebugFlag)
        }

        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlin.coroutines.core)
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.material.icons.extended)
            implementation(libs.ui)
            implementation(libs.resources)
            implementation(libs.tooling)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
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
            implementation(libs.paging.compose)
            implementation(libs.navigation.compose)
            implementation(libs.adaptive)
            implementation(libs.adaptive.layout)
            implementation(libs.adaptive.navigation)
            implementation(libs.koalaplot.core)
            implementation(libs.nanohttpd)
            implementation(libs.androidx.room.runtime)
            implementation(libs.kotlinx.immutable)
            implementation(libs.qrcode)
            implementation(libs.cryptohash)
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
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}


dependencies {
//    "baselineProfile"(projects.baselineprofile)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
    generateKotlin = true
}

buildkonfig {
    packageName = APP_ID

    // default config is required
    defaultConfigs {
        buildConfigField(STRING, "APP_NAME", APP_NAME, const = true)
        buildConfigField(STRING, "APP_ID", APP_ID, const = true)
        buildConfigField(INT, "VER_CODE", VER_CODE.toString(), const = true)
        buildConfigField(STRING, "VER_NAME", VER_NAME, const = true)
        buildConfigField(STRING, "CHANGELOG", CHANGELOG, const = true)
        buildConfigField(BOOLEAN, "DEBUG", (!isReleaseBuild).toString(), const = true)

    }

    targetConfigs {
        // names in create should be the same as target names you specified
        create("android") {
//            buildConfigField(STRING, "nullableField", "NonNull-value", nullable = true)
        }

        create("desktop") {
            buildConfigField(
                INT, "OS_ORDINAL",
                when {
                    os.isWindows -> "0"
                    os.isMacOsX -> "1"
                    os.isLinux -> "2"
                    else -> throw IllegalStateException("Unsupported OS: $os")
                }, const = true
            )

            buildConfigField(STRING, "OS_ARCH", resourcesDirName, const = true)
        }
    }
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
    }

    exports {
        create("desktop") {
            outputFile = file("src/desktopMain/composeResources/files/aboutlibraries.json")
        }
    }

}

compose.desktop {
    application {
        mainClass = "com.arn.scrobble.main.MainKt"

        val libraryPathRel = if (isReleaseBuild)
            "\$APPDIR/resources"
        else
            File(
                project.layout.projectDirectory.dir("resources").asFile,
                resourcesDirName
            ).absolutePath

        val libraryPath = File(libraryPathRel).absolutePath

        // ZGC starts with ~70MB minimized and goes down to ~160MB after re-minimizing
        jvmArgs += listOfNotNull(
            "-Dpano.native.components.path=$libraryPath",
            "--enable-native-access=ALL-UNNAMED",
            if (os.isLinux) "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED" else null,
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
                os.isWindows -> mutableSetOf(TargetFormat.AppImage)
                os.isLinux -> mutableSetOf(TargetFormat.AppImage)
                os.isMacOsX -> mutableSetOf(TargetFormat.Dmg)
                else -> throw IllegalStateException("Unsupported OS: $os")
            }

            targetFormats = formats
            packageVersion = VER_NAME
            vendor = "kawaiiDango"
            packageName = APP_NAME_NO_SPACES

            appResourcesRootDir = project.layout.projectDirectory.dir("resources")

            windows {
                dirChooser = true
                upgradeUuid = "85173f4e-ca52-4ec9-b77f-c2e0b1ff4209"
                msiPackageVersion = packageVersion
                exePackageVersion = packageVersion
                menuGroup = APP_NAME
                description = APP_NAME_NO_SPACES
                iconFile = file("app-icons/pano-scrobbler.ico")
            }

            linux {
                menuGroup = APP_NAME_NO_SPACES
                description = APP_NAME_NO_SPACES
                iconFile = file("app-icons/pano-scrobbler.png")
            }

            macOS {
                bundleID = APP_ID
                packageName = APP_NAME
                dockName = APP_NAME
                description = APP_NAME
                appStore = false

                iconFile = file("app-icons/pano-scrobbler.icns")
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

tasks.withType<ComposeHotRun>().configureEach {
    val libraryPath =
        File(project.layout.projectDirectory.dir("resources").asFile, resourcesDirName).absolutePath

    isAutoReloadEnabled = true
    mainClass = "com.arn.scrobble.main.MainKt"
    jvmArgs = (jvmArgs ?: emptyList()) + listOfNotNull(
        "-Dpano.native.components.path=$libraryPath",
        "-Dcompose.application.configure.swing.globals=true",
        "--enable-native-access=ALL-UNNAMED",
        if (os.isLinux) "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED" else null,
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

    val appDataDir = File(appDataRoot, "$APP_NAME_NO_SPACES-debug").absolutePath
    args = listOf("--data-dir", appDataDir)
}

tasks.register<DefaultTask>("generateSha256") {
    val distDir = file("../dist")

    doLast {
        distDir.listFiles { file ->
            file.isFile && !file.name.endsWith(".sha256") && !file.name.endsWith(".txt")
        }?.map { file ->
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            "$hash  ${file.name}"
        }?.let {
            val text = it.joinToString("\n")
            val sumFile = File(distDir, "sha256sums.txt")
            sumFile.writeText(text)

            println("sha256sums.txt generated")
        }
    }
}

//tasks.register<Zip>("zipAppImage") {
//    from("build/compose/binaries/main-release/app")
//    archiveFileName = "$appNameWithoutSpaces-$resourcesDirName.zip"
//    destinationDirectory = file("dist")
//}


tasks.register<Copy>("copyReleaseDmg") {
    val fileName = "$APP_NAME_NO_SPACES-$resourcesDirName.dmg"
    from("build/compose/binaries/main-release/dmg")
    into("../dist")
    include("*.dmg")
    rename(
        "(.*).dmg",
        fileName
    )
}

tasks.register<Exec>("packageWindowsNsis") {
    val executableDir = file("build/compose/native/$resourcesDirName")
    val nsisFilesDir = file("nsis-files")
    val distDir = file("../dist")

    val distFile = File(distDir, "$APP_NAME_NO_SPACES-$resourcesDirName.exe")
    val nsisScriptFile = File(nsisFilesDir, "install-script.nsi")
    val iconFile = file("app-icons/pano-scrobbler.ico")

    val nsisDir = System.getenv("PROGRAMFILES(x86)") + "\\NSIS"

    doFirst {
        distFile.parentFile.mkdirs()

        // create install.log with relative paths of all regular files under executableDir
        if (executableDir.exists()) {
            val installLogFile = File(executableDir, "install.log")
            val lines = mutableListOf<String>()
            Files.walk(executableDir.toPath()).use { stream ->
                stream.filter {
                    Files.isRegularFile(it) && it.name != installLogFile.name
                }
                    .forEach { p ->
                        val rel = executableDir.toPath().relativize(p).toString()
                        lines.add(rel)
                    }
            }
            lines.sort()
            installLogFile.writeText(lines.joinToString("\n"))
        }
    }

    commandLine(
        "\"$nsisDir\\makensis\"",
        "/DOUTFILE=" + distFile.absolutePath,
        "/DAPPDIR=" + executableDir.absolutePath,
        "/DVERSION_CODE=$VER_CODE",
        "/DVERSION_NAME=$VER_NAME",
        "/DICON_FILE=" + iconFile.absolutePath,
        nsisScriptFile.absolutePath
    )
}

tasks.register<Exec>("packageLinuxAppImageAndTarball") {
    commandLine(
        "bash",
        "../package-for-linux.sh",
    )
}

// graalvm plugin doesnt seem to support this project structure, so directly use the command
tasks.register<Exec>("buildNativeImage") {
    val graalvmHome = System.getenv("GRAALVM_HOME")
    val javaHome = System.getenv("JAVA_HOME")
    val copyDesktopAndIcon = os.isLinux

    val jarFile =
        file("build/compose/jars/$APP_NAME_NO_SPACES-$resourcesDirName-$VER_NAME.jar")
    val jarTree = zipTree(jarFile)
    val jarFilesToExtract = if (os.isWindows && arch in archAmd64)
        arrayOf("skiko-windows-x64.dll", "icudtl.dat", "natives/windows_x64/sqliteJni.dll")
    else if (os.isLinux && arch in archAmd64)
        arrayOf("libskiko-linux-x64.so", "natives/linux_x64/libsqliteJni.so")
    else if (os.isLinux && arch in archArm64)
        arrayOf("libskiko-linux-arm64.so", "natives/linux_arm64/libsqliteJni.so")
    else
        arrayOf()

    val outputDir = file("build/compose/native/$resourcesDirName")
    val outputFile = File(outputDir, APP_NAME_NO_SPACES)

    val rechabilityFilesSuffix = if (Runtime.version().version().first() < 24) "-21" else ""
    val reachabilityFiles = file(
        "rechability-metadata${rechabilityFilesSuffix}/" +
                if (os.isWindows) "windows" else "linux"
    )
    val jawtDirName = if (os.isWindows)
        "bin"
    else
        "lib"
    val jawtDir = File(outputDir, jawtDirName)
    val jawtFile = when {
        os.isWindows -> file("$graalvmHome/bin/jawt.dll")
        os.isLinux -> file("$graalvmHome/lib/libjawt.so")
        else -> throw IllegalStateException("Unsupported OS: $os")
    }

    val winAppResFile = file("app-icons/exe-res.res")

    val nativeLibsDir = file("resources/$resourcesDirName/")
    val iconFile = file("src/commonMain/composeResources/drawable/ic_launcher_with_bg.svg")
    val desktopFile = file("$APP_NAME_NO_SPACES.desktop")
    val licenseFile = file("../LICENSE")
    val distDir = file("../dist")

    inputs.file(jarFile)
    inputs.dir(nativeLibsDir)
    inputs.dir(reachabilityFiles)
    inputs.file(licenseFile)

    outputs.dir(outputDir)

    val command = listOfNotNull(
        if (os.isWindows)
            "$graalvmHome\\bin\\native-image.cmd"
        else
            "$graalvmHome/bin/native-image",
        "--strict-image-heap",
        "--no-fallback",
        "-march=" + if (arch in archArm64) "armv8.1-a" else "x86-64-v2",
        if (os.isLinux && arch in archArm64) "-H:PageSize=16384" else null,
        if (os.isLinux) "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED" else null,
        "-H:+UnlockExperimentalVMOptions",
        "-J-Dcompose.application.configure.swing.globals=true",
        "-J-Djava.awt.headless=false",
        "-J-Dfile.encoding=UTF-8",
        "-J-Dsun.java2d.dpiaware=true",
        "-H:ConfigurationFileDirectories=" + reachabilityFiles.absolutePath,
        "-R:MaxHeapSize=300M",
        "--initialize-at-build-time=kotlin.text.Charsets",
        "-H:+AddAllCharsets",
        "-H:+ReportExceptionStackTraces",
//        "-g",
//        "--enable-monitoring=nmt",
        "--enable-native-access=ALL-UNNAMED",
        if (Runtime.version().version().first() < 24) null else "-H:+ForeignAPISupport",
        if (Runtime.version().version().first() < 24)
            "-H:+IncludeAllLocales"
        else
            "--include-locales",
        if (os.isWindows) "-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS" else null,
        if (os.isWindows) "-H:NativeLinkerOption=/ENTRY:mainCRTStartup" else null,
        if (os.isWindows) "-H:NativeLinkerOption=\"${winAppResFile.absolutePath}\"" else null,
        "-jar",
        jarFile.absolutePath,
        "-o",
        outputFile.absolutePath,
    )

    commandLine(command)

    doFirst {
        // env check
        if (graalvmHome.isNullOrEmpty() || graalvmHome != javaHome) {
            throw GradleException("GRAALVM_HOME should be set and should be equal to JAVA_HOME")
        }
        outputDir.mkdirs()
        distDir.mkdirs()
    }

    doLast {
//        println("Executing command:")
//        println(command.joinToString(" "))
        // copy jawt
        jawtDir.mkdirs()
        jawtFile.copyTo(File(jawtDir, jawtFile.name), overwrite = true)

        val otherJawtFile = File(outputDir, jawtFile.name)
        if (otherJawtFile.exists())
            otherJawtFile.delete()

        // copy native components
        nativeLibsDir.copyRecursively(
            outputDir,
            overwrite = true
        )

        // extract jni libraries from .jar
        jarTree.matching {
            include(*jarFilesToExtract)
        }.forEach { file ->
            file.copyTo(File(jawtDir, file.name), overwrite = true)
        }

        licenseFile.copyTo(File(outputDir, licenseFile.name), overwrite = true)

        // copy icon and desktop file on linux
        if (copyDesktopAndIcon) {
            iconFile.copyTo(File(outputDir, "pano-scrobbler.svg"), overwrite = true)
            desktopFile.copyTo(File(outputDir, desktopFile.name), overwrite = true)
        }
    }
}

afterEvaluate {
    if (os.isMacOsX)
        tasks.named("packageReleaseDmg") {
            finalizedBy(tasks.named("copyReleaseDmg"))
        }

    tasks.named("packageUberJarForCurrentOS") {
        finalizedBy(tasks.named("buildNativeImage"))
    }

    if (os.isLinux)
        tasks.named("buildNativeImage") {
            finalizedBy(tasks.named("packageLinuxAppImageAndTarball"))
        }

    if (os.isWindows)
        tasks.named("buildNativeImage") {
            finalizedBy(tasks.named("packageWindowsNsis"))
        }

    tasks.named("copyNonXmlValueResourcesForAndroidMain") {
        // Ensure AboutLibraries export runs first
        dependsOn(":androidApp:exportLibraryDefinitions")
        // Optional ordering guard
        mustRunAfter(":androidApp:exportLibraryDefinitions")
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
            val newLocaleUtilsText =
                localeUtilsText.take(start) + localeUtilsPartialText + localeUtilsText.substring(end)
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
                            // escape apostrophes
                            val textNodes = stringElement.childNodes
                            for (j in 0 until textNodes.length) {
                                val textNode = textNodes.item(j)
                                if (textNode.nodeType == org.w3c.dom.Node.TEXT_NODE) {
                                    textNode.nodeValue =
                                        textNode.nodeValue.replace("(?<!\\\\)'".toRegex(), "\\\\'")
                                }
                            }

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
                            // escape apostrophes
                            val textNodes = pluralElement.childNodes
                            for (j in 0 until textNodes.length) {
                                val textNode = textNodes.item(j)
                                if (textNode.nodeType == org.w3c.dom.Node.TEXT_NODE) {
                                    textNode.nodeValue.replace("(?<!\\\\)'".toRegex(), "\\\\'")
                                }
                            }

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