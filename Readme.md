## Pano Scrobbler

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
- Supports LastFM, LibreFM, GNU FM, and Listenbrainz
- Supports phones, TVs, tablets, and Android desktops, including Windows 11
- Interactive notification: View song info, edit, love, cancel, or block songs directly from the
  notification
- View song, album, artist, album artist, and tag details
- View scrobbles from a specific time, such as last year or last month
- Edit or delete existing scrobbles. Remembers edits
- Extract or Fix metadata such as "Remastered" with regex pattern edits
- Block artists, songs, etc., and automatically skip or mute when they play
- Check what your followed users are listening to and view their stats
- Identify and scrobble a song from the microphone
- Scrobble from the S app and Pixel Now Playing
- Import and export settings, edits, and blocklists
- View charts for a specific week, month, year, or custom range, with change indicators
- Charts are also available as a customizable home-screen widget
- Collage generator
- View scrobble count graphs for specific time periods
- Get a random songs, album, or artist from your listening history
- Search for a songs, artist, or album
- Fetch albums and album artists from LastFM, if missing, before scrobbling
- Get your top scrobble digests as a notification at the end of every week and month
- Add or remove personal tags
- Mix and match themes, supports Material You
- Broadcast Intents for automation apps like Tasker
- Remember and see apps you scrobbled from and search directly within them

### Building:

See [instructions.md](instructions.md)

- Inspiration from Musixmatch's FloatingLyrics
- Lastfm API wrapper: Based on https://github.com/jkovacs/lastfm-java
- Icons: https://materialdesignicons.com and https://material.io
- Genres filter from https://everynoise.com

Thanks to the amazing [translators](https://crowdin.com/project/pscrobbler/members) and everyone who
reported bugs and helped me with this project.
