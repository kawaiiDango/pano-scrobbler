Welcome to my spaghetti

Create /app/src/main/java/com/arn/scrobble/Tokens.kt
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
    const val SPOTIFY_ARTIST_INFO_SERVER = "" // deprecated, leave it empty [self hosted server](https://github.com/kawaiidango/spotify-artist-search-server)
    const val SPOTIFY_ARTIST_INFO_KEY = "" // deprecated, leave it empty
    const val PRO_PRODUCT_ID = "" // play store product ID for IAP
    const val SIGNATURE = "" // apk signature
    const val BASE_64_ENCODED_PUBLIC_KEY = "" // (of the signing key)
}
```
Remove the lines below `// remove if not needed` in app/build.gradle.kts and /build.gradle.kts

Create app/version.txt and put a positive integer in it

Create a Firebase project for Crashlytics and add google-services.json

See https://firebase.google.com/docs/android/setup