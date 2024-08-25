pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "pano-scrobbler"
include(":app")
include(":baselineprofile")
include(":extras-common")
include(":extras-play")
include(":extras-foss")