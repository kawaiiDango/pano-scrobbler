# Build Instructions

Welcome to my spaghetti

- Create /app/src/main/java/com/arn/scrobble/Tokens.kt
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
    const val APK_SIGNATURE = ""
    const val PLAY_BILLING_PUBLIC_KEY_BASE64 = ""
    const val EMBEDDED_SERVER_KEYSTORE_PASSWORD = "" // password for the embedded https server BKS keystore, used for importing settings over local network
}
```
- Remove or comment out the lines below `// remove if not needed` in app/build.gradle.kts and /build.gradle.kts

- Create app/version.txt and put a positive integer in it. This will be your app version code.
The version name will be derived from this.

- Create a Firebase project for Crashlytics and add google-services.json.
See https://firebase.google.com/docs/android/setup

- Create a BKS keystore for the embedded https server with the password EMBEDDED_SERVER_KEYSTORE_PASSWORD and alias selfsigned.
Put it in /app/src/main/res/raw/embedded_server_bks.bks

- Obtain now playing notification strings and their translations by decompiling the resources of
the Android System Intelligence apk with ApkTool and then running [py-scripts/np-strings-extract.py](py-scripts/np-strings-extract.py) on them.

Usage: `python ./np-strings-extract.py <decompiled-dir> song_format_string np` for scrobbling Pixel Now Playing.
    
Alternatively, you can use this as a stub in `strings.xml`:
```
<string name="song_format_string">%1$s by %2$s</string>
```

- If you want to generate the optional baseline profile for the app, which can improve its startup time,
create a file `/baselineprofile/src/main/java/com/arn/scrobble/baselineprofile/Secrets.kt`:
```
object Secrets {
    const val type = "lastfm"
    const val username = "<lastfmUsername>"
    const val sessionKey = "<lastfmSessionKey>"
}
```

sessionKey can be obtained by logging in to LastFM with a debug build of this app
and tapping on the "Copy last.fm session key" in the settings.