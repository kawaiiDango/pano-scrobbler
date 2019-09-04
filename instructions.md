Get [lastfm](https://www.last.fm/api/account/create) and [ACRCloud](https://console.acrcloud.com/) api tokens
and add the following to /app/src/main/java/com/arn/scrobble/Tokens.kt
```
package com.arn.scrobble
object Tokens {
    const val LAST_KEY = ""
    const val LAST_SECRET = ""
    const val ACR_KEY = ""
    const val ACR_SECRET = ""
}
```
Remove line 5 from app/build.gradle

Create a file app/version.txt and put `1` in it
