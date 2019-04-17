Get the lastfm api tokens (https://www.last.fm/api/account/create)
and add the following to /app/src/main/java/com/arn/scrobble/Tokens.kt
```
package com.arn.scrobble
object Tokens {
    const val LAST_KEY = "your key"
    const val LAST_SECRET = "your secret"
}
```
Remove line 5 from app/build.gradle

Create a file app/version.txt and put `1` in it
