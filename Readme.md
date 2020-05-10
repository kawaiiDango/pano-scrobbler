## Pano Scrobbler
[play-store]: shields/play-store.svg
[play-store-link]: https://play.google.com/store/apps/details?id=com.arn.scrobble
[ko-fi]: shields/ko-fi.svg
[ko-fi-link]: https://ko-fi.com/kawaiiDango
[![download][play-store]][play-store-link] [![donate][ko-fi]][ko-fi-link]

<img alt='GIF' src='https://media.giphy.com/media/WvikAmG3iseJoFun1A/giphy.gif' width="240"/>
<i>Thats the GIF of a much older version.</i>

<a href="https://bit.ly/Pscrdl">Download latest apk</a> (Do not use this if the play store link works for you and you want automatic updates)

#### Features:
- Scrobble from anything which produces audio with metadata, including Youtube, Chrome, Telegram etc.
- Scrobble to lastFM, LibreFM, GNU FM and Listenbrainz
- View scrobble count in notification
- Sanity check the metadata with lastFM's records. 
- View track info, such as listener count charts, album art etc
- View recent scrobbles
- Work with recent scrobbles (love a track, cancel a scrobble)
- Edit scrobbles for free
- Delete existing scrobbles
- Offline cacheing and scrobbling
- Check what your friends are listening to
- View and play songs similar to a given one
- Identify and scrobble a song from mic
- Supports TVs (Oreo and lower needs adb commands)
- Supports Pixel Now Playing

#### This is not intended to:
- be an app to browse lastfm.
- use webviews except the first steps. Other links would rather open in the browser
- contain any kind of ADs

### Building:
See [instructions.md](instructions.md)

### Thanks to:
- Musixmatch's FloatingLyrics for the idea and inspiration
- https://github.com/jkovacs/lastfm-java (I added many missing lastfm api methods in a messy way, might make a pull request some time)
- http://materialdesignicons.com
- Other dependencies in build.gradle
- **Kaycee da Ghost** for putting up with me while i tried to fix miui specific bugs
- **memeko ヾ(\*・ω・)ノ** for the ui design
- **My username is true** as a long time dedicated tester and spotting lots of bugs
- **nabz** for ui and graphics design ideas
- **nomad** for ui and graphics design ideas and fixes
- everyone who reported bugs in-app, and helped me fix crashes and scrobbling issues