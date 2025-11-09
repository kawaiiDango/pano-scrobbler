# Build Instructions

Welcome to my spaghetti

- Create composeApp/src/commonMain/kotlin/com/arn/scrobble/Tokens.kt

```
package com.arn.scrobble

object Tokens {
    const val LAST_KEY = "" // https://www.last.fm/api/account/create
    const val LAST_SECRET = ""
    // https://developer.spotify.com/dashboard/
    const val SPOTIFY_REFRESH_TOKEN = "<base64 encoded client_id:client_secret>"
    const val PLAY_BILLING_PUBLIC_KEY_BASE64 = "" // Use an empty string if you aren't using Play Billing
}
```

### For Android:

- Create a Firebase project for Crashlytics and add the google-services.json to composeApp/
  See https://firebase.google.com/docs/android/setup. This is not required for building the
  releaseGithub (FOSS) variant.

- If you want to generate the optional custom baseline profile for the app, which can improve its
  startup time, create a file
  `/baselineprofile/src/main/java/com/arn/scrobble/baselineprofile/Secrets.kt`:

```
object Secrets {
    const val type = "lastfm"
    const val username = "<lastfmUsername>"
    const val sessionKey = "<lastfmSessionKey>"
}
```

sessionKey can be obtained by logging in to LastFM with a debug build of this app
and tapping on the "Copy last.fm session key" in the settings screen.

Then run `./gradlew :baselineprofile:generateBaselineProfile`

Currently, the FOSS builds skip this step.

- Run `./gradlew composeApp:exportLibraryDefinitions -PaboutLibraries.exportVariant=release` and
`./gradlew androidApp:assembleRelease` for the Play Store build

- or `./gradlew composeApp:exportLibraryDefinitions -PaboutLibraries.exportVariant=releaseGithub` and
`./gradlew androidApp:assembleReleaseGithub` for the FOSS build

- Info: the build variant "release" includes Google Play billing, Crashlytics (has opt out) and
  Google Play Review API as its non-free dependencies, while the build variant "releaseGithub"
  includes no non-free dependencies.

### For desktop:

- Compile the native lib https://github.com/kawaiiDango/pano-native-components

- If you intend to package a build for desktop,
  use [Bellsoft's GraalVM based on OpenJDK 25](https://bell-sw.com/pages/downloads/native-image-kit/)
  as your gradle JDK and run gradle packageUberJarForCurrentOS. Also
  have [NSIS](https://nsis.sourceforge.io/Main_Page) installed in Program Files on Windows.

- Run `./gradlew composeApp:exportLibraryDefinitions -PaboutLibraries.exportVariant=desktop`
- Run `./gradlew composeApp:packageUberJarForCurrentOS`

- Info: the desktop build includes no non-free dependencies.
