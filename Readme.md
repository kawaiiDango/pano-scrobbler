## Pano Scrobbler
[play-store]: shields/play-store.svg
[play-store-link]: https://play.google.com/store/apps/details?id=com.arn.scrobble

[ko-fi]: shields/ko-fi.svg
[ko-fi-link]: https://ko-fi.com/kawaiiDango

[crowdin]: shields/crowdin.svg
[crowdin-link]: https://crowdin.com/project/pscrobbler

[![download][play-store]][play-store-link] [![translate][crowdin]][crowdin-link]

<img src="https://i.imgur.com/XGmtOP5.jpg" alt="scrobbles" width="200"/> <img src="https://i.imgur.com/m1lbrU6l.jpg" alt="friends" width="200"/> <img src="https://i.imgur.com/7kETocF.jpg" alt="charts" width="200"/>

### PC app?
Pano Scrobbler works on Windows 11 with WSA. It scrobbles from other Android apps.
Buttons in notifications don't work as expected. This is a WSA bug.

### Features:
- No ADs ever
- Scrobbles to LastFM, LibreFM, GNU FM and Listenbrainz (A LastFM login is required for now)
- Supports phones, TVs, tablets and Android desktops including Windows 11
- Interactive notification: View scrobble counts, love, cancel or block tracks directly from the notification
- View track, album, artist, album artist and tag details
- View scrobbles from a specific time such as last year, last month etc.
- Edit or delete existing scrobbles. Remembers edits
- Fix metadata such as "Remastered" or your own patterns with regex edits
- Block artists, tracks etc and auto skip or mute when they play
- Check what your friends are listening to and view their stats
- Identify and scrobble a song from the microphone
- Scrobble from AutoShazam and Pixel Now Playing
- Import & export settings, edits and blocklists
- View charts for a specific week, month, year or custom range, with change indicators
- Charts are also available as a customizable home-screen widget
- Collage generator
- View scrobble count graphs for specific time periods
- Get a random track, album or artist from your listening history
- Search for a track, artist or album
- Fetch albums and album artists from LastFM, if missing, before scrobbling
- Get your top scrobble digests as a notification at the end of every week and month
- Add or remove personal tags
- Mix and match themes, supports Material You
- Broadcast Intents for automation apps like Tasker
- Show scrobble sources beside each scrobble and directly search in the app you scrobbled from
- In-app update checker for devices without Play Store

### Building:
See [instructions.md](instructions.md)

- Inspiration from: Musixmatch's FloatingLyrics
- Lastfm API wrapper: Based on https://github.com/jkovacs/lastfm-java
- Icons: https://materialdesignicons.com and https://material.io

Thanks to everyone who reported bugs and helped me with this project.