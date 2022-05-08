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
    const val SPOTIFY_ARTIST_INFO_SERVER = "" // blank or [self hosted server](https://github.com/kawaiidango/spotify-artist-search-server)
    const val SPOTIFY_ARTIST_INFO_KEY = ""
    const val PRO_SKU_NAME = ""
    const val SIGNATURE = "apk signature"
    const val BASE_64_ENCODED_PUBLIC_KEY = ""
}
```
Remove the lines below `// remove if not needed` in app/build.gradle.kts and /build.gradle.kts

Create app/version.txt and put a positive integer in it
