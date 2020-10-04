Get [lastfm](https://www.last.fm/api/account/create) and [ACRCloud](https://console.acrcloud.com/) api tokens
and add the following to /app/src/main/java/com/arn/scrobble/Tokens.kt
```
package com.arn.scrobble
object Tokens {
    const val LAST_KEY = ""
    const val LAST_SECRET = ""
    const val ACR_HOST = ""
    const val ACR_KEY = ""
    const val ACR_SECRET = ""
    const val SPOTIFY_ARTIST_INFO_SERVER = "" // blank or (self hosted server)[https://github.com/kawaiidango/spotify-artist-search-server]
    const val SPOTIFY_ARTIST_INFO_KEY = ""
}
```
Remove line 5 from app/build.gradle
Remove line 11 from ./build.gradle

Create a file app/version.txt and put `1` in it
