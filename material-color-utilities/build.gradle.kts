import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(25)
}

interface InjectedExec {
    @get:Inject
    val execOps: ExecOperations
}

tasks.register("syncMaterialColorUtilities") {
    val repoUrl = "https://github.com/material-foundation/material-color-utilities.git"

    val commitFile = layout.projectDirectory.file("material-color-utilities-commit.txt")
    val dest = layout.projectDirectory.file("src/main/kotlin")
    val gitCloneDir = layout.buildDirectory.file("material-color-utilities")
    val execOps = project.objects.newInstance<InjectedExec>()

    doLast {
        val packages = listOf(
            "hct",
            "palettes/TonalPalette.kt",
            "utils/ColorUtils.kt",
            "utils/MathUtils.kt",
            "dynamiccolor",
            "scheme/SchemeCmf.kt",
            "scheme/SchemeExpressive.kt",
            "scheme/SchemeTonalSpot.kt",
            "scheme/SchemeVibrant.kt",
            "dislike",
            "contrast",
            "temperature"
        )

        val requestedRef = commitFile.asFile.readText().trim()

        fun runGit(vararg args: String) {
            val out = ByteArrayOutputStream()
            val result = execOps.execOps.exec {
                workingDir = gitCloneDir.get().asFile.also { it.mkdirs() }
                standardOutput = out
                errorOutput = out
                isIgnoreExitValue = true
                commandLine(listOf("git") + args)
            }

            if (result.exitValue != 0) {
                throw GradleException(
                    "git ${args.joinToString(" ")} failed:\n${
                        out.toString(Charsets.UTF_8)
                    }"
                )
            }
        }

        try {
            println("Fetching material-color-utilities @ $requestedRef")
            runGit(
                "clone", "--no-checkout", repoUrl, ".",
            )

            val packagePaths = packages.map { "kotlin/$it" }
            runGit(
                "checkout", requestedRef, "--", *packagePaths.toTypedArray()
            )

            dest.asFile.deleteRecursively()

            for (pkg in packages) {
                val src = gitCloneDir.get().asFile.resolve("kotlin").resolve(pkg)
                val target = dest.asFile.resolve(pkg)
                src.copyRecursively(target, overwrite = true)
            }
        } finally {
            gitCloneDir.get().asFile.deleteRecursively()
        }
    }
}