import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.StrictMode
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.compose.reload.gradle.ComposeHotRun

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.graalvm.native)
}

val APP_ID = rootProject.extra["APP_ID"] as String
val VER_NAME = rootProject.extra["VER_NAME"] as String
val APP_NAME_NO_SPACES = rootProject.extra["APP_NAME_NO_SPACES"] as String
val RESOURCES_DIR_NAME = rootProject.extra["RESOURCES_DIR_NAME"] as String
val IS_WINDOWS = rootProject.extra["IS_WINDOWS"] as Boolean
val IS_LINUX = rootProject.extra["IS_LINUX"] as Boolean
val IS_X64 = rootProject.extra["IS_X64"] as Boolean
val IS_ARM64 = rootProject.extra["IS_ARM64"] as Boolean

val needsGraalvm = gradle.startParameter.taskNames.any { requested ->
    requested.substringAfterLast(":").contains("packageNativeImage", ignoreCase = true)
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(25)
        if (needsGraalvm)
            vendor = JvmVendorSpec.matching("GraalVM Community")
    }
}

dependencies {
    implementation(projects.composeApp)
}

fun commonJvmArgs(): List<String> {
    val libPath = File(
        project.layout.projectDirectory.dir("resources").asFile,
        RESOURCES_DIR_NAME
    ).absolutePath

    val iconsPath = File(
        project.layout.projectDirectory.dir("app-icons").asFile,
        RESOURCES_DIR_NAME.substringBefore("-")
    ).absolutePath

    return listOfNotNull(
        "-Dpano.native.components.path=$libPath",
        "-Dpano.icons.path=$iconsPath",
        "--enable-native-access=ALL-UNNAMED",
        if (IS_LINUX) "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED" else null,
        "-Dfile.encoding=UTF-8",
        "-Dnative.encoding=UTF-8",
//        "-XX:+UseSerialGC",
//        "-Xms32m",
//        "-Xmx512m",
//        "-XX:NativeMemoryTracking=detail",
    )
}

compose.desktop {
    application {
        mainClass = "com.arn.scrobble.main.MainKt"
        jvmArgs += commonJvmArgs()
//        args += "-m"

        nativeDistributions {
            packageVersion = VER_NAME
            vendor = "kawaiiDango"
            packageName = APP_NAME_NO_SPACES
        }
    }
}

tasks.withType<ComposeHotRun>().configureEach {
    isAutoReloadEnabled = true
    mainClass = "com.arn.scrobble.main.MainKt"
    jvmArgs = commonJvmArgs()

    val appDataRoot = when {
        IS_WINDOWS -> {
            System.getenv("APPDATA")?.ifEmpty { null }
                ?: System.getProperty("user.home")
        }

        IS_LINUX -> {
            System.getenv("XDG_DATA_HOME")?.ifEmpty { null }
                ?: (System.getProperty("user.home") + "/.local/share")
        }

        else -> throw IllegalStateException("unsupported os")
    }

    val appDataDir = File(appDataRoot, "$APP_NAME_NO_SPACES-debug").absolutePath
    args = listOf("--data-dir", appDataDir)
}

if (IS_WINDOWS) {
    tasks.register<Exec>("packageInno") {
        val executableDir = layout.buildDirectory.dir("native/$RESOURCES_DIR_NAME")
        val distDir = layout.projectDirectory.file("../dist")
        val scriptFile = layout.projectDirectory.file("inno/installer.iss")
        val iconFile = layout.projectDirectory.file("app-icons/windows/pano-scrobbler.ico")
        val isccPath = System.getenv("PROGRAMFILES") + "\\Inno Setup 7\\ISCC.exe"
        val isccPathUser = System.getenv("LOCALAPPDATA") + "\\Programs\\Inno Setup 7\\ISCC.exe"

        inputs.dir(executableDir)
        inputs.file(scriptFile)
        inputs.file(iconFile)
        outputs.file(layout.projectDirectory.file("../dist/$APP_NAME_NO_SPACES-$RESOURCES_DIR_NAME-setup.exe"))

        doFirst {
            distDir.asFile.mkdirs()
        }

        commandLine(
            if (File(isccPath).exists()) isccPath else isccPathUser,
            "/DOUT_DIR=" + distDir.asFile.absolutePath,
            "/DAPP_DIR=" + executableDir.get().asFile.absolutePath,
            "/DVERSION=$VER_NAME",
            "/DICON_FILE=" + iconFile.asFile.absolutePath,
            scriptFile.asFile.absolutePath
        )
    }
}

if (IS_LINUX) {
    tasks.register<Exec>("packageLinuxReleases") {
        inputs.dir(layout.buildDirectory.dir("native/$RESOURCES_DIR_NAME"))
        inputs.file(layout.projectDirectory.file("package-for-linux.sh"))
        outputs.files(
            layout.projectDirectory.file("../dist/$APP_NAME_NO_SPACES-$RESOURCES_DIR_NAME.tar.zst"),
            layout.projectDirectory.file("../dist/$APP_NAME_NO_SPACES-$RESOURCES_DIR_NAME.deb"),
            layout.projectDirectory.file("../dist/$APP_NAME_NO_SPACES-$RESOURCES_DIR_NAME.AppImage")
        )

        commandLine(
            "bash",
            "package-for-linux.sh",
        )
    }
}

if (IS_WINDOWS) {
    tasks.register<Exec>("generateRc") {

        val rcTemplateFile = file("rc-template.txt")
        val rcOutputDir = project.layout.buildDirectory.dir("generated-rc").get().asFile
        val outputFileName = "$APP_NAME_NO_SPACES.exe"
        val rcOut = File(rcOutputDir, "$outputFileName.rc")
        val versionMajor = VER_NAME.substringBefore(".")
        val versionMinor = VER_NAME.substringAfter(".")

        // find rc.exe
        val rcExe = File(System.getenv("PROGRAMFILES(x86)") + "\\Windows Kits\\10\\bin")
            .listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("10.") }
            ?.maxByOrNull { it.lastModified() }
            ?.let { File(it, "x64\\rc.exe") }
            ?.absolutePath

        if (rcExe == null)
            throw GradleException("rc.exe not found. Please install Windows 10 SDK.")

        // compile rc to res
        val command = listOf(
            rcExe,
            "/nologo",
            rcOut.absolutePath
        )

        commandLine(command)

        doFirst {
            val rcContent = rcTemplateFile
                .readText()
                .replace("\$versionMajor", versionMajor)
                .replace("\$versionMinor", versionMinor)
                .replace("\$fileName", outputFileName)

            rcOutputDir.mkdirs()
            rcOut.writeText(rcContent)
        }
    }
}


val copyReachabilityMetadata = tasks.register<Copy>("copyReachabilityMetadata") {
    val osDir = RESOURCES_DIR_NAME.substringBefore("-")
    from("rechability-metadata/$osDir")
    into(layout.buildDirectory.dir("generated/reachability-metadata/META-INF/native-image/$APP_ID/$APP_NAME_NO_SPACES"))
}

kotlin.sourceSets.getByName("main").resources.srcDir(
    copyReachabilityMetadata.map {
        it.destinationDir.parentFile.parentFile.parentFile.parentFile
        // points to: generated/reachability-metadata/
    }
)

graalvmNative {
    metadataRepository {
        enabled = false
    }

    agent {
        //   ./gradlew :desktopApp:run -Pagent
        //   ./gradlew :desktopApp:metadataCopy --task run -Pagent
        enabled = false
        defaultMode = "standard"

        builtinCallerFilter = true
        builtinHeuristicFilter = true
        enableExperimentalPredefinedClasses = false
        enableExperimentalUnsafeAllocationTracing = false
        trackReflectionMetadata = true

        metadataCopy {
            inputTaskNames.add("run")
            val osDir = RESOURCES_DIR_NAME.substringBefore("-")
            outputDirectories.add("rechability-metadata/$osDir-tmp")
            mergeWithExisting = false
        }
    }

    binaries {
        named("main") {
            imageName = APP_NAME_NO_SPACES
            mainClass = "com.arn.scrobble.main.MainKt"

            val localesTextFile = file("../composeApp/locales.txt")
            val winAppResFile =
                project.layout.buildDirectory.file("generated-rc/$APP_NAME_NO_SPACES.exe.res")

            buildArgs.addAll(
                listOfNotNull(
                    if (IS_X64) "-march=x86-64-v2" else null,
                    if (IS_LINUX && IS_ARM64) "-H:PageSize=16384" else null,
                    if (IS_LINUX) "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED" else null,
                    "-H:+UnlockExperimentalVMOptions",
                    "-J-Djava.awt.headless=false",
                    "-J-Dfile.encoding=UTF-8",
                    "-J-Dnative.encoding=UTF-8",
                    "-J-Dsun.java2d.dpiaware=true",
                    "--exact-reachability-metadata",
                    "-H:MissingRegistrationReportingMode=Warn",
                    "-R:MaxHeapSize=300M",
                    "--initialize-at-build-time=kotlin.text.Charsets",
                    "-H:+AddAllCharsets",
                    "-H:+ReportExceptionStackTraces",
                    "--enable-native-access=ALL-UNNAMED",
//                    "--include-locales",
                    "-H:IncludeLocales=\"" + localesTextFile.readLines().joinToString(",") + "\"",
                    if (IS_WINDOWS) "-J-Djavax.net.ssl.trustStore=NONE" else null,
                    if (IS_WINDOWS) "-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS" else null,
                    if (IS_WINDOWS) "-H:NativeLinkerOption=/ENTRY:mainCRTStartup" else null,
                    if (IS_WINDOWS) "-H:NativeLinkerOption=\"${winAppResFile.get().asFile.absolutePath}\"" else null,
                )
            )
        }
    }
}

tasks.register<Sync>("packageNativeImage") {
    dependsOn("nativeCompile")

    finalizedBy(
        when {
            IS_LINUX -> "packageLinuxReleases"
            IS_WINDOWS -> "packageInno"
            else -> throw GradleException("OS not supported")
        }
    )
    val graalvmLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(25)
        if (needsGraalvm)
            vendor = JvmVendorSpec.matching("GraalVM Community")
    }
    val graalvmHome = graalvmLauncher.get().metadata.installationPath.asFile.absolutePath
    val copyDesktopAndIcon = IS_LINUX

    val jarFilesToExtract = if (IS_WINDOWS && IS_X64)
        arrayOf("skiko-windows-x64.dll", "icudtl.dat", "natives/windows_x64/sqliteJni.dll")
    else if (IS_LINUX && IS_X64)
        arrayOf("libskiko-linux-x64.so", "natives/linux_x64/libsqliteJni.so")
    else if (IS_LINUX && IS_ARM64)
        arrayOf("libskiko-linux-arm64.so", "natives/linux_arm64/libsqliteJni.so")
    else
        arrayOf()

    val nativeLibJars: FileCollection = configurations
        .named("runtimeClasspath")
        .get()
        .incoming
        .artifactView {
            componentFilter { id ->
                id is ModuleComponentIdentifier && (
                        (id.group == "org.jetbrains.skiko" && id.module == "skiko-awt-runtime-all") ||
                                (id.group == "androidx.sqlite" && id.module == "sqlite-bundled-jvm")
                        )
            }
        }
        .files

    val archiveOperations = serviceOf<ArchiveOperations>()

    val filesToDelete = arrayOf(
        "libjsound.so",
        "jsound.dll",
        "libjavajpeg.so",
        "javajpeg.dll",
        "liblcms.so",
        "lcms.dll",
    )

    // Default output location of the graalvmNative plugin's nativeCompile task
    val nativeCompileOutputDir = layout.buildDirectory.dir("native/nativeCompile")
    val outputDir = layout.buildDirectory.dir("native/$RESOURCES_DIR_NAME").get().asFile

    val jawtDirName = if (IS_WINDOWS) "bin" else "lib"
    val jawtDir = File(outputDir, jawtDirName)
    val jawtFile = when {
        IS_WINDOWS -> file("$graalvmHome/bin/jawt.dll")
        IS_LINUX -> file("$graalvmHome/lib/libjawt.so")
        else -> throw IllegalStateException("Unsupported OS")
    }

    val nativeLibsDir = file("resources/$RESOURCES_DIR_NAME/")
    val mainIconFile =
        file("../composeApp/src/jvmMain/composeResources/drawable/ic_launcher_with_bg.svg")
    val linuxOtherIconsDir = file("app-icons/linux")
    val desktopFile = file("$APP_NAME_NO_SPACES.desktop")
    val licenseFile = file("../LICENSE")
    val distDir = file("../dist")

    // Sync the raw native-image build output into our packaging dir first
    from(nativeCompileOutputDir)
    into(outputDir)

    doFirst {
        outputDir.mkdirs()
        distDir.mkdirs()
    }

    doLast {
        // copy jawt
        jawtDir.mkdirs()
        jawtFile.copyTo(File(jawtDir, jawtFile.name), overwrite = true)

        val otherJawtFile = File(outputDir, jawtFile.name)
        if (otherJawtFile.exists())
            otherJawtFile.delete()

        // copy native components
        nativeLibsDir.copyRecursively(outputDir, overwrite = true)

        nativeLibJars.forEach { jar ->
            archiveOperations.zipTree(jar).matching {
                include(*jarFilesToExtract)
            }.forEach { file ->
                file.copyTo(File(jawtDir, file.name), overwrite = true)
            }
        }

        licenseFile.copyTo(File(outputDir, licenseFile.name), overwrite = true)

        // copy icon and desktop file on linux
        if (copyDesktopAndIcon) {
            val iconsOutputDir = File(outputDir, "icons")
            iconsOutputDir.mkdirs()
            mainIconFile.copyTo(
                File(iconsOutputDir, "hicolor/scalable/apps/pano-scrobbler.svg"),
                overwrite = true
            )
            linuxOtherIconsDir.copyRecursively(iconsOutputDir, overwrite = true)
            desktopFile.copyTo(File(outputDir, desktopFile.name), overwrite = true)
        }

        // delete unnecessary files
        filesToDelete.forEach { fileName ->
            File(outputDir, fileName).takeIf { it.exists() }?.delete()
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
        outputFile =
            file("../composeApp/src/jvmMain/composeResources/files/aboutlibraries.json")
    }
}

tasks.configureEach {
    when (name) {
        "exportLibraryDefinitions" -> {
            finalizedBy(":composeApp:copyNonXmlValueResourcesForJvmMain")
        }

        "packageNativeImage" -> {
            if (IS_LINUX)
                finalizedBy("packageLinuxReleases")
            else if (IS_WINDOWS)
                finalizedBy("packageInno")
        }

//        "packageUberJarForCurrentOS" -> {
//        }

        "nativeCompile" -> {
            dependsOn("exportLibraryDefinitions")
//            dependsOn("packageUberJarForCurrentOS")
            if (IS_WINDOWS)
                dependsOn("generateRc")
        }
    }
}