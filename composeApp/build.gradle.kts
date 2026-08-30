import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import com.google.gson.Gson
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.encoding.Base64

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildkonfig)
}

val isReleaseBuild = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true) ||
            it.contains("packageUberJarForCurrentOS", ignoreCase = true) ||
            it.contains("packageNativeImage", ignoreCase = true)
}

val APP_ID = rootProject.extra["APP_ID"] as String
val VER_CODE = rootProject.extra["VER_CODE"] as Int
val VER_NAME = rootProject.extra["VER_NAME"] as String
val APP_NAME = rootProject.extra["APP_NAME"] as String
val APP_NAME_NO_SPACES = rootProject.extra["APP_NAME_NO_SPACES"] as String
val RESOURCES_DIR_NAME = rootProject.extra["RESOURCES_DIR_NAME"] as String
val IS_WINDOWS = rootProject.extra["IS_WINDOWS"] as Boolean
val IS_LINUX = rootProject.extra["IS_LINUX"] as Boolean

val localProperties = gradleLocalProperties(rootDir, project.providers)
    .map { it.key to it.value.toString() }
    .toMap()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xreturn-value-checker=check")
    }

    android {
        compileSdk {
            version = release(libs.versions.targetSdk.get().toInt()) {
//                minorApiLevel = libs.versions.sdkMinor.get().toInt()
            }
        }
        namespace = APP_ID
        minSdk = libs.versions.minSdk.get().toInt()

        androidResources {
            enable = true
        }

        withHostTest {}
    }

    jvm()

    sourceSets {

        androidMain.dependencies {
//            implementation(libs.core)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.activity.compose)
            implementation(libs.work.runtime)
            implementation(libs.core.remoteviews)
            implementation(libs.coil.gif)
            implementation(libs.qrcode)
            implementation(libs.webkit)
            implementation(projects.extrasAndroid)
        }

        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.resources)
            implementation(libs.tooling.preview)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.lifecycle.viewmodel.nav3)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.okhttp)
            implementation(libs.aboutlibraries.core)
            implementation(libs.kotlin.csv)
            implementation(libs.kermit)
            implementation(libs.datastore.core)
            implementation(libs.paging.common)
            implementation(libs.paging.compose)
            implementation(libs.nav3.ui)
            implementation(libs.adaptive)
            implementation(libs.adaptive.layout)
            implementation(libs.adaptive.nav3)
            implementation(libs.koalaplot.core)
            implementation(libs.nanohttpd)
            implementation(libs.room.runtime)
            implementation(libs.cryptohash)
            implementation(projects.extrasCommon)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(projects.extrasNonplay)
            implementation(libs.sqlite.bundled)
            implementation(libs.jmdns)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}


dependencies {
//    "baselineProfile"(projects.baselineprofile)
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    "androidRuntimeClasspath"(libs.tooling)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

buildkonfig {
    fun xor(text: String, key: String): String {
        require(text.isNotEmpty()) { "Key bytes must not be empty" }
        val data = text.toByteArray()
        val keyBytes = key.toByteArray()
        val out = ByteArray(data.size)
        val klen = keyBytes.size
        for (i in data.indices) {
            val a = data[i].toInt() and 0xFF
            val b = keyBytes[i % klen].toInt() and 0xFF
            out[i] = (a xor b).toByte()
        }

        return Base64
            .withPadding(Base64.PaddingOption.ABSENT)
            .encode(out)
    }

    packageName = APP_ID

    // default config is required
    defaultConfigs {
        buildConfigField(STRING, "APP_NAME", APP_NAME, const = true)
        buildConfigField(STRING, "APP_ID", APP_ID, const = true)
        buildConfigField(INT, "VER_CODE", VER_CODE.toString(), const = true)
        buildConfigField(STRING, "VER_NAME", VER_NAME, const = true)
        buildConfigField(BOOLEAN, "DEBUG", (!isReleaseBuild).toString(), const = true)

        val lastfmKey = localProperties["lastfm.key"]
            ?: throw IllegalStateException("lastfm.key not found in local.properties")

        val lastfmSecret = localProperties["lastfm.secret"]
            ?: throw IllegalStateException("lastfm.secret not found in local.properties")

        val spotifyRefreshToken = localProperties["spotify.refreshToken"]
            ?: throw IllegalStateException("spotify.refreshToken not found in local.properties")

        buildConfigField(
            STRING,
            "LASTFM_KEY",
            xor(lastfmKey, APP_ID),
            const = true
        )
        buildConfigField(
            STRING,
            "LASTFM_SECRET",
            xor(lastfmSecret, APP_ID),
            const = true
        )
        buildConfigField(
            STRING,
            "SPOTIFY_REFRESH_TOKEN",
            xor(spotifyRefreshToken, APP_ID),
            const = true
        )

    }

    targetConfigs {
        // names in create should be the same as target names you specified
        create("android") {
//            buildConfigField(STRING, "nullableField", "NonNull-value", nullable = true)
        }

        create("jvm") {
            buildConfigField(BOOLEAN, "IS_WINDOWS", IS_WINDOWS.toString(), const = true)
            buildConfigField(BOOLEAN, "IS_LINUX", IS_LINUX.toString(), const = true)
            buildConfigField(STRING, "OS_ARCH", RESOURCES_DIR_NAME, const = true)
        }
    }
}

tasks.register("updateMaterialSymbols") {
    val symbolsDir = layout.buildDirectory.dir("material-symbols-svgs").get().asFile
    outputs.dir(symbolsDir)

    val unfilledNamesFile = file("material-symbols-names/unfilled.txt")
    val filledNamesFile = file("material-symbols-names/filled.txt")
    val automirroredNamesFile = file("material-symbols-names/automirrored.txt")

    inputs.files(
        unfilledNamesFile,
        filledNamesFile,
        automirroredNamesFile
    )

    doLast {
        fun buildUrl(iconName: String, filled: Boolean): String {
            val fillName = if (filled) "fill1" else "default"
            val variantName = "rounded"
            // Google Fonts official CDN
            return "https://fonts.gstatic.com/s/i/short-term/release/materialsymbols$variantName/$iconName/$fillName/24px.svg"
        }

        fun downloadBatch(
            iconNames: List<String>,
            filled: Boolean,
            outputDir: File
        ) {
            iconNames.forEach { iconName ->
                val url = buildUrl(iconName, filled)
                val iconFile = File(outputDir, "$iconName.svg")
                URI(url).toURL().openStream().use { input ->
                    iconFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                println("Downloaded: $iconName")
            }
        }

        downloadBatch(
            unfilledNamesFile
                .readLines().distinct(),
            filled = false,
            symbolsDir
        )

        downloadBatch(
            filledNamesFile.readLines().distinct(),
            filled = true,
            File(symbolsDir, "filled").also { it.mkdir() }
        )

        downloadBatch(
            automirroredNamesFile.readLines().distinct(),
            filled = false,
            File(symbolsDir, "automirrored").also { it.mkdir() }
        )

        println("Material Symbols download completed.")
    }
}

tasks.register<Exec>("convertMaterialSymbols") {
    val inputDir = layout.buildDirectory.dir("material-symbols-svgs").get().asFile
    val outputDir = file("src/commonMain/kotlin/com/arn/scrobble/icons")
    val cliPath = file(
        "valkyrie-cli/bin/valkyrie" +
                (if (IS_WINDOWS) ".bat" else "")
    )

    val pkgName = APP_ID + ".icons"

    val shellCmd = if (IS_WINDOWS)
        listOf(
            "cmd.exe",
            "/c",
        )
    else
        listOf(
            "bash",
            "-c",
        )

    val iconPack = listOf(
        cliPath.absolutePath,
        "iconpack",
        "--output-path=" + outputDir.absolutePath,
        "--package-name=" + pkgName,
        "--iconpack=" + "Icons.Filled,Icons.AutoMirrored",
    )

    val mainIcons = listOf(
        cliPath.absolutePath,
        "svgxml2imagevector",
        "--input-path=" + inputDir.absolutePath,
        "--output-path=" + outputDir.absolutePath,
        "--package-name=" + pkgName,
        "--iconpack-name=" + "Icons",
    )

    val filledIcons = listOf(
        cliPath.absolutePath,
        "svgxml2imagevector",
        "--input-path=" + File(inputDir, "filled").absolutePath,
        "--output-path=" + outputDir.absolutePath,
        "--package-name=" + pkgName,
        "--iconpack-name=" + "Icons",
        "--nested-pack-name=" + "Filled",
    )

    val autoMirroredIcons = listOf(
        cliPath.absolutePath,
        "svgxml2imagevector",
        "--input-path=" + File(inputDir, "automirrored").absolutePath,
        "--output-path=" + outputDir.absolutePath,
        "--package-name=" + pkgName,
        "--iconpack-name=" + "Icons",
        "--nested-pack-name=" + "AutoMirrored",
        "--auto-mirror=" + "true",
    )

    // run all of them in a single command to avoid multiple exec tasks
    commandLine(
        shellCmd + listOf(
            (iconPack + "&&" + mainIcons + "&&" + filledIcons + "&&" + autoMirroredIcons)
                .joinToString(" ")
        )
    )
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
            URI("https://api.crowdin.com/api/v2/projects/$projectId/members?limit=500&orderBy=username&role=translator").toURL()
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
    val localesTextFile = file("locales.txt")

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
            URI("https://api.crowdin.com/api/v2/projects/$projectId/languages/progress?limit=500").toURL()
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

            val localesWithNames = languagesFiltered.map {
                val localeObj = Locale.forLanguageTag(it)
                val displayLanguage = localeObj.getDisplayLanguage(localeObj)

                val suffix = when (localeObj.language) {
                    "zh" -> " " + localeObj.getDisplayScript(localeObj)
                    "pt" -> localeObj.getDisplayCountry(localeObj)
                        .ifEmpty { null }
                        ?.let { " $it" } ?: ""

                    else -> ""
                }

                it to displayLanguage + suffix
            }.sortedWith { (k1, v1), (k2, v2) ->
                v1.compareTo(v2, ignoreCase = true)
            }

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
    val localesMap = mapOf(
${localesWithNames.joinToString(",\n") { "        \"${it.first}\" to \"${it.second}\"" }}
    )
"""

            val localeUtilsText = localeUtilsFile.readText()
            val start =
                localeUtilsText.indexOf("// localesSet start") + "// localesSet start".length
            val end = localeUtilsText.indexOf("    // localesSet end")
            val newLocaleUtilsText =
                localeUtilsText.take(start) + localeUtilsPartialText + localeUtilsText.substring(
                    end
                )
            localeUtilsFile.writeText(newLocaleUtilsText)

            // write to locales.txt
            localesTextFile.writeText(languagesFiltered.joinToString("\n"))

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
                val docBuilderFactory = DocumentBuilderFactory.newInstance()
                val docBuilder = docBuilderFactory.newDocumentBuilder()
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
                                        textNode.nodeValue.replace(
                                            "(?<!\\\\)'".toRegex(),
                                            "\\\\'"
                                        )
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

                // Use LSSerializer to control line endings (LF only)
                val domImpl = docBuilderFactory.newDocumentBuilder()
                    .domImplementation
                val domImplLS =
                    domImpl.getFeature("LS", "3.0") as org.w3c.dom.ls.DOMImplementationLS
                val serializer = domImplLS.createLSSerializer()

                val config = serializer.domConfig
                config.setParameter("format-pretty-print", true)

                serializer.newLine = "\n"
                val lsOutput = domImplLS.createLSOutput()
                targetFile.outputStream().use { fos ->
                    lsOutput.byteStream = fos
                    lsOutput.encoding = "UTF-8"
                    serializer.write(targetDoc, lsOutput)
                }
            }
        }
    }
}

tasks.register<Copy>("copyMds") {
    val files = arrayOf("faq.md", "changelog.md")

    from(project.layout.projectDirectory.dir("../"))
    into(project.layout.projectDirectory.dir("src/commonMain/composeResources/files"))

    include(*files)
    inputs.files(project.layout.projectDirectory.files(*files))
    outputs.files(
        project.layout.projectDirectory
            .dir("src/commonMain/composeResources/files")
            .files(*files)
    )
}

tasks.configureEach {
    when (name) {
        "copyNonXmlValueResourcesForCommonMain" -> {
            dependsOn("copyMds")
        }

        "updateMaterialSymbols" -> {
            finalizedBy("convertMaterialSymbols")
        }
    }
}