# Build Instructions

Welcome to my spaghetti

Put these in local.properties
```
# // https://www.last.fm/api/account/create
lastfm.key=
lastfm.secret=
# // https://developer.spotify.com/dashboard/
spotify.refreshToken=<base64 encoded client_id:client_secret>
```

### For Android (FOSS):

- Run `./gradlew androidApp:assembleReleaseGithub`

- **Info**: the FOSS build includes no non-free dependencies.

### For Android (Play Store):

- Create a Firebase project for Crashlytics and add the google-services.json to composeApp/
  See https://firebase.google.com/docs/android/setup

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

Currently, the builds skip this step.

- Run `./gradlew androidApp:assembleRelease`

- **Info**: the Play Store variant includes Google Play billing, Crashlytics (has opt-out) and
  Google Play Review API as its non-free dependencies

### For desktop:

- Compile the native lib https://github.com/kawaiiDango/pano-native-components

- If you intend to package a build for desktop,
  use [Bellsoft's GraalVM based on OpenJDK 25](https://bell-sw.com/pages/downloads/native-image-kit/)
  as your JAVA_HOME and GRAALVM_HOME (both should be set). Also
  have [Inno Setup](https://jrsoftware.org/isdl.php) installed in Program Files on Windows.

- Run `./gradlew composeApp:exportLibraryDefinitions composeApp:packageUberJarForCurrentOS -PaboutLibraries.exportVariant=desktop`

- **Info**: the desktop build includes no non-free dependencies.
