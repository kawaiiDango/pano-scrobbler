# Build Instructions

Welcome to my spaghetti

- Create composeApp/src/commonMain/kotlin/com/arn/scrobble/Tokens.kt

```
package com.arn.scrobble
object Tokens {
    const val LAST_KEY = "" // https://www.last.fm/api/account/create
    const val LAST_SECRET = ""
    // https://developer.spotify.com/dashboard/
    const val SPOTIFY_REFRESH_TOKEN = "<base64 of spotify client id>:<base64 of spotify client secret>"
    // returns {"code": 0, "message": "valid"} if the license is valid, this spec is subject to change
    const val LICENSE_CHECKING_SERVER = "https://"
    // RS256 public key, used to verify the license
    const val LICENSE_PUBLIC_KEY_BASE64 = ""
    const val PLAY_BILLING_PUBLIC_KEY_BASE64 = ""
    // password for the embedded https server BKS/JKS keystore, used for importing settings over local network
    const val EMBEDDED_SERVER_KEYSTORE_PASSWORD = ""
}
```

- Comment out the lines below `// remove if not needed` in composeApp/build.gradle.kts

- To generate the licenses files, run

```
./gradlew composeApp:exportLibraryDefinitions -PaboutLibraries.exportVariant=releaseGithub
./gradlew composeApp:exportLibraryDefinitions -PaboutLibraries.exportVariant=release
./gradlew composeApp:exportLibraryDefinitions -PaboutLibraries.exportVariant=desktop
```


### For Android:

- Create a Firebase project for Crashlytics and add the google-services.json to composeApp/
  See https://firebase.google.com/docs/android/setup. This is not required for building the
  releaseGithub (FOSS) variant.

- Create a BKS keystore for the embedded https server used for the import/export feature over local
  network, with the password you used in EMBEDDED_SERVER_KEYSTORE_PASSWORD and alias selfsigned.
  Put it in composeApp/src/androidMain/composeResources/files/embedded_server.bks

- Obtain now playing notification strings and their translations by decompiling the resources of
  the Android System Intelligence apk with ApkTool and then
  running [py-scripts/np-strings-extract.py](py-scripts/np-strings-extract.py) on them.

Usage: `python ./np-strings-extract.py <decompiled-dir> song_format_string np` for scrobbling Pixel
Now Playing.

Alternatively, you can use this as a stub in `strings.xml`:

```
<string name="song_format_string">%1$s by %2$s</string>
```

- If you want to generate the optional baseline profile for the app, which can improve its startup
  time, create a file `/baselineprofile/src/main/java/com/arn/scrobble/baselineprofile/Secrets.kt`:

```
object Secrets {
    const val type = "lastfm"
    const val username = "<lastfmUsername>"
    const val sessionKey = "<lastfmSessionKey>"
}
```

sessionKey can be obtained by logging in to LastFM with a debug build of this app
and tapping on the "Copy last.fm session key" in the settings screen.

- Run ./gradlew composeApp:assembleRelease or ./gradlew composeApp:assembleReleaseGithub

- Info: the build variant "release" includes Google Play billing, Crashlytics (has opt out) and
  Google Play Review API as its non-free dependencies, while the build variant "releaseGithub"
  includes no non-free dependencies.

### For desktop:

- Convert the previously created BKS keystore to a JKS keystore, or create a new one for the
  embedded https server used for the import/export feature over local network, with the password you
  used in EMBEDDED_SERVER_KEYSTORE_PASSWORD and the alias selfsigned.
  Put it in composeApp/src/desktopMain/composeResources/files/embedded_server.bks

- Compile the native lib https://github.com/kawaiiDango/pano-native-components

- If you intend to package a build for desktop,
  use [Bellsoft's GraalVM based on OpenJDK 25](https://bell-sw.com/pages/downloads/native-image-kit/)
  as your gradle JDK and run gradle packageUberJarForCurrentOS. Also
  have [makensis.exe](https://nsis.sourceforge.io/Main_Page) in your PATH for Windows.

- Run ./gradlew composeApp:packageUberJarForCurrentOS

- Info: the desktop build includes no non-free dependencies.
