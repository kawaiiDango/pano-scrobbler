# Build Instructions

Welcome to my spaghetti

- Create composeApp/src/commonMain/kotlin/com/arn/scrobble/Tokens.kt

```
package com.arn.scrobble
object Tokens {
    const val LAST_KEY = "" // https://www.last.fm/api/account/create
    const val LAST_SECRET = ""
    const val ACR_HOST = "" // https://console.acrcloud.com/
    const val ACR_KEY = ""
    const val ACR_SECRET = ""
    // https://developer.spotify.com/dashboard/
    const val SPOTIFY_REFRESH_TOKEN = "<base64 of spotify client id>:<base64 of spotify client secret>"
    // returns {"code": 0, "message": "valid"} if the license is valid, this spec is subject to change
    const val LICENSE_CHECKING_SERVER = "https://"
    // used to verify the license
    const val LICENSE_PUBLIC_KEY_BASE64 = ""
    const val PLAY_BILLING_PUBLIC_KEY_BASE64 = ""
    const val EMBEDDED_SERVER_KEYSTORE_PASSWORD = "" // password for the embedded https server BKS keystore, used for importing settings over local network
}
```

- Comment out the lines below `// remove if not needed` in composeApp/build.gradle.kts

- To generate the licenses file, run
  `composeApp:exportLibraryDefinitions -PaboutLibraries.exportPath=src/commonMain/composeResources/files/ -PaboutLibraries.exportVariant=release`

### For Android:

- Create a Firebase project for Crashlytics and add the google-services.json to composeApp/
  See https://firebase.google.com/docs/android/setup

- Create a BKS keystore for the embedded https server used for the import/export feature over local
  network,
  with the password you used in EMBEDDED_SERVER_KEYSTORE_PASSWORD and alias selfsigned.
  Put it in composeApp/src/commonMain/composeResources/files/embedded_server.bks

- Obtain now playing notification strings and their translations by decompiling the resources of
  the Android System Intelligence apk with ApkTool and then
  running [py-scripts/np-strings-extract.py](py-scripts/np-strings-extract.py) on them.

Usage: `python ./np-strings-extract.py <decompiled-dir> song_format_string np` for scrobbling Pixel
Now Playing.

Alternatively, you can use this as a stub in `strings.xml`:

```
<string name="song_format_string">%1$s by %2$s</string>
```

- To copy some android specific strings to android resources, from common resources, run the gradle
  task `copyStringsToAndroid`

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

- Info: the build variant "release" includes Google Play billing, Crashlytics (has opt out), Google
  Play Review API and AcrCloud as its non-free dependencies, while the build variant "releaseGithub"
  includes AcrCloud as its only non-free dependency.

### For desktop:

- If you intend to package a build for desktop, use OpenJDK 21 as your gradle JDK, as the built-in
  one does not contain jpackage
