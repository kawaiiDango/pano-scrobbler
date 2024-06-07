## <img src="app/src/main/play/listings/en-US/icon/icon.png" alt="app icon" width="32"/> Pano Scrobbler

[play-store]: shields/play-store.svg

[play-store-link]: https://play.google.com/store/apps/details?id=com.arn.scrobble

[ko-fi]: shields/ko-fi.svg

[ko-fi-link]: https://ko-fi.com/kawaiiDango

[crowdin]: shields/crowdin.svg

[crowdin-link]: https://crowdin.com/project/pscrobbler

[![download][play-store]][play-store-link] [![translate][crowdin]][crowdin-link]

<img src="https://i.imgur.com/pgETfhc.png" alt="scrobbles" width="150"/> <img src="https://i.imgur.com/Q7yPi2z.png" alt="friends" width="150"/> <img src="https://i.imgur.com/MUhcyBw.png" alt="charts" width="150"/> <img src="https://i.imgur.com/aikbtGR.png" alt="random" width="150"/>

### PC app?

Pano Scrobbler works on Windows 11 with WSA and can scrobble from other Android apps.
However, there is a known issue with the buttons in notifications not functioning as expected,
which is a bug specific to WSA.

### Features:

- No ads ever
- Scrobbles to LastFM, LibreFM, GNU FM, Listenbrainz, Maloja, Pleroma
- * Scrobble to a CSV or JSONL file locally
- Supports phones, TVs, tablets, and Android desktops, including Windows 11
- * Interactive notification: View song info, edit, love, cancel, or block songs directly from the
  notification
- View song, album, artist, album artist, and tag details
- View scrobbles from a specific time, such as last year or last month
- Edit or delete existing scrobbles. Remembers edits
- Extract or Fix metadata such as "Remastered" with regex pattern edits
- Block artists, songs, etc., and automatically skip or mute when they play
- Check what your followed users are listening to and view their stats
- Identify and scrobble a song from the microphone
- * Scrobble from the S app and Pixel Now Playing
- Import and export settings, edits, and blocklists
- View charts with change indicators for specific time periods,
- * Charts are also available as a customizable home-screen widget
- * Collage generator
- View scrobble count graphs and tag clouds
- Get a random songs, album, or artist from your listening history
- Search for a songs, artist, or album
- Fetch albums and album artists from LastFM, if missing, before scrobbling
- * Get your top scrobble digests as a notification at the end of every week and month
- Add or remove personal tags
- Mix and match themes, supports Material You
- * Broadcast Intents for automation apps like Tasker
- Remember and see apps you scrobbled from and search directly within them

Features marked with * are not available on TV due to OS limitations.


### Compiling

See [instructions.md](instructions.md)

### F-droid repo?
[https://kawaiidango.github.io/pano-scrobbler/fdroid/repo/
](https://kawaiidango.github.io/pano-scrobbler/fdroid/repo?fingerprint=9954ECAB27F9FCE8290AC75A33F3DFE9FE5F6F5B8E6F33AD7F98307AC4D487BA)

<img src="https://kawaiidango.github.io/pano-scrobbler/fdroid/repo/index.png" alt="QR code of the fdroid repo" width="200"/>


### Credits
- Inspiration from Musixmatch's FloatingLyrics
- YouTube title parser from [Web Scrobbler](https://github.com/web-scrobbler/web-scrobbler) and [Metadata Filter](https://github.com/web-scrobbler/metadata-filter)
- Icons from https://pictogrammers.com and https://material.io
- Genres filter from https://everynoise.com

Thanks to the amazing [translators](app/src/main/res/raw/crowdin_members.json) and everyone who
reported bugs and helped me with this project.
