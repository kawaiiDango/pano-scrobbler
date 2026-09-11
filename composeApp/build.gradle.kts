import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import groovy.json.JsonSlurper
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale
import java.util.zip.GZIPInputStream
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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xreturn-value-checker=check")
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.FlowPreview")
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

        lint {
            toolchain {
                languageVersion = JavaLanguageVersion.of(25)
            }
        }

        withHostTest {}
    }

    jvm()
    jvmToolchain(25)

    sourceSets {

        androidMain.dependencies {
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
            implementation(projects.materialColorUtilities)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
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

        val lastfmKey = localProperties.getProperty("lastfm.key")
            ?: throw IllegalStateException("lastfm.key not found in local.properties")

        val lastfmSecret = localProperties.getProperty("lastfm.secret")
            ?: throw IllegalStateException("lastfm.secret not found in local.properties")

        val spotifyRefreshToken = localProperties.getProperty("spotify.refreshToken")
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
    val symbolsDir = layout.buildDirectory.dir("material-symbols").get().asFile
    outputs.dir(symbolsDir)

    val unfilledNamesFile = file("material-symbols-names/unfilled.txt")
    val filledNamesFile = file("material-symbols-names/filled.txt")
    val automirroredNamesFile = file("material-symbols-names/automirrored.txt")
    val filledAutomirroredNamesFile = file("material-symbols-names/filled_automirrored.txt")

    inputs.files(
        unfilledNamesFile,
        filledNamesFile,
        automirroredNamesFile,
        filledAutomirroredNamesFile
    )

    doLast {
        val httpClient = HttpClient.newHttpClient()
        var count = 0

        fun buildUrl(iconName: String, filled: Boolean): String {
            val fill = if (filled) 1 else 0
            // Google Fonts official CDN, these are always gzipped
            return "https://fonts.gstatic.com/render/v1/Material+Symbols+Rounded/24dp/$iconName.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,$fill,0,50"
        }

        fun downloadBatch(
            iconNames: List<String>,
            filled: Boolean,
            autoMirrored: Boolean,
        ) {
            iconNames.forEach { iconName ->
                val url = buildUrl(iconName, filled)
                val fileNameSuffix =
                    (if (filled) " filled" else "") + (if (autoMirrored) " autoMirrored" else "")
                val iconFile = File(symbolsDir, "$iconName$fileNameSuffix.kt")

                val request = HttpRequest.newBuilder(URI(url))
                    .GET()
                    .setHeader("Accept-Encoding", "gzip")
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())

                val contentEncoding = response.headers().firstValue("Content-Encoding").orElse("")

                val inputStream = if (contentEncoding.equals("gzip", ignoreCase = true)) {
                    GZIPInputStream(response.body())
                } else {
                    response.body()
                }

                inputStream.use { input ->
                    iconFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                count++
            }
        }

        downloadBatch(
            unfilledNamesFile.readLines().distinct(),
            filled = false,
            autoMirrored = false,
        )

        downloadBatch(
            filledNamesFile.readLines().distinct(),
            filled = true,
            autoMirrored = false,
        )

        downloadBatch(
            automirroredNamesFile.readLines().distinct(),
            filled = false,
            autoMirrored = true,
        )

        downloadBatch(
            filledAutomirroredNamesFile.readLines().distinct(),
            filled = true,
            autoMirrored = true,
        )

        println("Downloaded: $count icons")
    }
}

tasks.register("processMaterialSymbols") {
    val inputDir = layout.buildDirectory.dir("material-symbols").get().asFile
    val outputDir = file("src/commonMain/kotlin/com/arn/scrobble/icons")
    val pkgName = "$APP_ID.icons"

    doLast {
        val inputFiles = inputDir.listFiles { file ->
            file.isFile && file.extension == "kt"
        } ?: return@doLast

        outputDir.mkdirs()
        var count = 0

        val iconObjFile = File(outputDir, "Icons.kt")
        val iconObjText = """
            package $pkgName

            object Icons
        """.trimIndent()
        iconObjFile.writeText(iconObjText)

        inputFiles.forEach { inputFile ->
            val sourceIconName = inputFile.nameWithoutExtension.substringBefore(" ")
            val autoMirroredMarker = " autoMirrored"
            val destIconName = inputFile.nameWithoutExtension
                .split("_", " ")
                .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }

            val text = inputFile.readText()
                .replaceFirst("package .+".toRegex(), "package $pkgName")
                .replaceFirst("\"$sourceIconName\"", "\"$destIconName\"")
                .replace("_$sourceIconName", "_$destIconName")
                .replaceFirst("PathFillType.Companion.", "PathFillType.")
                .replace(
                    """public val\s+(?!Icons\.)([A-Za-z_][A-Za-z0-9_]*)\s*:\s*ImageVector\b""".toRegex(),
                    "val Icons.$destIconName: ImageVector"
                )
                .let {
                    if (inputFile.nameWithoutExtension.contains(autoMirroredMarker))
                        it.replace("""ImageVector\.Builder\(([\s\S]*?)\n(\s*)\)(\s*\n\s*\.apply)""".toRegex()) { match ->
                            val params = match.groupValues[1]
                            val closingIndent = match.groupValues[2]
                            val apply = match.groupValues[3]
                            val paramIndent = "$closingIndent  "

                            "ImageVector.Builder($params\n$paramIndent" + "autoMirror = true,\n$closingIndent)$apply"
                        }
                    else
                        it
                }

            val outputFile = File(outputDir, "$destIconName.kt")
            outputFile.writeText(text)

            count++
        }

        println("Processed $count icons.")
    }
}

@Suppress("UNCHECKED_CAST")
tasks.register("fetchCrowdinMembers") {
    val projectIdProvider = project.provider { localProperties.getProperty("crowdin.project")!! }
    val tokenProvider = project.provider { localProperties.getProperty("crowdin.token")!! }
    val outputFile = file("src/commonMain/composeResources/files/crowdin_members.txt")
    val debugFile = layout.buildDirectory.file("crowdin/top-members-report.json")

    outputs.files(outputFile, debugFile)

    doLast {
        val projectId = projectIdProvider.get()
        val token = tokenProvider.get()
        val debugFile = debugFile.get().asFile
        var reportId: String? = null
//        reportId = "sth"

        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build()

        fun apiRequest(method: String, url: String, body: String? = null): String {
            val builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
            builder.method(
                method,
                if (body != null)
                    HttpRequest.BodyPublishers.ofString(body)
                else
                    HttpRequest.BodyPublishers.noBody()
            )
            val response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                throw GradleException("Crowdin API request failed [$method $url]: HTTP ${response.statusCode()} - ${response.body()}")
            }
            return response.body()
        }

        @Suppress("UNCHECKED_CAST")
        fun asMap(json: String): Map<String, Any?> =
            JsonSlurper().parseText(json) as Map<String, Any?>

        // generate a report
        val reportsBase = "https://api.crowdin.com/api/v2/projects/$projectId/reports"

        if (reportId == null) {

            val genBody = """
            {
              "name": "top-members",
              "schema": {
                "unit": "strings",
                "format": "json",
                "dateFrom": "2018-01-01T00:00:00+00:00",
                "dateTo": "${OffsetDateTime.now(ZoneOffset.UTC)}"
              }
            }
        """.trimIndent()

            var genData =
                asMap(apiRequest("POST", reportsBase, genBody))["data"] as Map<String, Any?>
            reportId = genData["identifier"] as String
            val statusUrl = "$reportsBase/$reportId"

            // poll until finished
            var status = genData["status"] as String
            var attempts = 0
            while (status != "finished") {
                if (status == "failed") throw GradleException("Crowdin report generation failed")
                if (attempts++ > 10) throw GradleException("Timed out waiting for Crowdin report")
                Thread.sleep(5000)
                val statusData = asMap(apiRequest("GET", statusUrl))["data"] as Map<String, Any?>
                status = statusData["status"] as String
            }
        }

        // Get the (pre-signed) download URL, then fetch the report content directly — no auth header needed here.
        val downloadDataUrl = "$reportsBase/$reportId/download"
        val downloadData =
            asMap(apiRequest("GET", downloadDataUrl))["data"] as Map<String, Any?>
        val downloadUrl = downloadData["url"] as String

        val reportResponse = client.send(
            HttpRequest.newBuilder().uri(URI.create(downloadUrl)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )
        if (reportResponse.statusCode() != 200) {
            throw GradleException("Failed to download Crowdin report: HTTP ${reportResponse.statusCode()}")
        }
        debugFile.parentFile.mkdirs()
        debugFile.writeText(reportResponse.body())

        // parse the report
        val report = JsonSlurper().parseText(reportResponse.body()) as Map<String, Any?>
        val entries = report["data"] as List<Map<String, Any?>>

        val translators = entries
            .filter { ((it["translated"] as? Number)?.toLong() ?: 0L) > 0 }
            .mapNotNull { (it["user"] as? Map<*, *>)?.get("username") as? String }
            .toSortedSet(String.CASE_INSENSITIVE_ORDER)

        outputFile.writeText(translators.joinToString("\n"))

        if (translators.isEmpty()) {
            println("No translators found, check the JSON")
        } else {
            println("Got ${translators.size} translators")
        }
    }
}

tasks.register("fetchCrowdinLanguages") {
    val projectIdProvider = project.provider { localProperties.getProperty("crowdin.project")!! }
    val tokenProvider = project.provider { localProperties.getProperty("crowdin.token")!! }
    val localesConfigFile = file("src/androidMain/res/xml/locales_config.xml")
    val localeUtilsFile = file("src/commonMain/kotlin/com/arn/scrobble/utils/LocaleUtils.kt")
    val localesTextFile = file("locales.txt")

    outputs.files(localesConfigFile, localeUtilsFile, localesTextFile)

    doLast {
        val projectId = projectIdProvider.get()
        val token = tokenProvider.get()
        val minProgress = 5
        val customMappings = mapOf(
            "zh-CN" to "zh-Hans",
            "pt-BR" to "pt-BR",
        )

        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build()

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.crowdin.com/api/v2/projects/$projectId/languages/progress?limit=500"))
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw IOException("Failed to fetch Crowdin languages. Response code: ${response.statusCode()}")
        }

        @Suppress("UNCHECKED_CAST")
        val root = JsonSlurper().parseText(response.body()) as Map<String, Any?>

        @Suppress("UNCHECKED_CAST")
        val entries = root["data"] as List<Map<String, Any?>>

        val languagesFiltered = (
                entries.mapNotNull { it["data"] as? Map<*, *> }
                    .filter {
                        ((it["translationProgress"] as? Number)?.toInt() ?: 0) >= minProgress
                    }
                    .map { data ->
                        val languageId = data["languageId"] as String
                        val twoLettersCode =
                            (data["language"] as Map<*, *>)["twoLettersCode"] as String
                        customMappings[languageId] ?: twoLettersCode
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
            localeUtilsText.take(start) + localeUtilsPartialText + localeUtilsText.substring(end)
        localeUtilsFile.writeText(newLocaleUtilsText)

        // write to locales.txt
        localesTextFile.writeText(languagesFiltered.joinToString("\n"))

        println("Got ${languagesFiltered.size} languages")
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
            finalizedBy("processMaterialSymbols")
        }
    }
}