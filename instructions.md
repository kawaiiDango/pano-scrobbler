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
    // password for the embedded https server PKCS #12 keystore, used for importing settings over local network
    const val EMBEDDED_SERVER_KEYSTORE_PASSWORD = ""
}
```

- Create a PKCS #12 keystore for the embedded https server used for the import/export feature over local
  network, with the password you used in EMBEDDED_SERVER_KEYSTORE_PASSWORD and alias selfsigned.

```
keytool -genkeypair \
  -alias selfsigned \
  -keystore pano-embedded-server-ks.p12 \
  -storetype PKCS12 \
  -keyalg RSA -keysize 2048 -sigalg SHA256withRSA -validity 3650 \
  -storepass <your-password> \
  -dname "CN=localhost, OU=IT, O=kawaiiDango, C=IN" \
  -ext "SAN=dns:localhost,ip:127.0.0.1" \
  -J-Dkeystore.pkcs12.keyProtectionAlgorithm=PBEWithSHA1AndDESede \
  -J-Dkeystore.pkcs12.certProtectionAlgorithm=PBEWithSHA1AndRC2_40 \
  -J-Dkeystore.pkcs12.macAlgorithm=HmacPBESHA1
```

Put it in composeApp/src/commonMain/composeResources/files/pano-embedded-server-ks.bks

- To generate the [aboutLibraries](https://github.com/mikepenz/AboutLibraries) files, run

```
./gradlew composeApp:exportLibraryDefinitions -PaboutLibraries.exportVariant=releaseGithub
./gradlew composeApp:exportLibraryDefinitions -PaboutLibraries.exportVariant=release
./gradlew composeApp:exportLibraryDefinitions -PaboutLibraries.exportVariant=desktop
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

- Run ./gradlew composeApp:assembleRelease or ./gradlew composeApp:assembleReleaseGithub

- Info: the build variant "release" includes Google Play billing, Crashlytics (has opt out) and
  Google Play Review API as its non-free dependencies, while the build variant "releaseGithub"
  includes no non-free dependencies.

### For desktop:

- Compile the native lib https://github.com/kawaiiDango/pano-native-components

- If you intend to package a build for desktop,
  use [Bellsoft's GraalVM based on OpenJDK 25](https://bell-sw.com/pages/downloads/native-image-kit/)
  as your gradle JDK and run gradle packageUberJarForCurrentOS. Also
  have [NSIS](https://nsis.sourceforge.io/Main_Page) installed in Program Files on Windows.

- Run ./gradlew composeApp:packageUberJarForCurrentOS

- Info: the desktop build includes no non-free dependencies.
