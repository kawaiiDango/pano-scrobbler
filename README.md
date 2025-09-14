## <img src="composeApp/src/commonMain/composeResources/drawable/ic_launcher_with_bg.svg" alt="app icon" width="32"/> Pano Scrobbler

[play-store]: img/play-store.svg
[play-store-link]: https://play.google.com/store/apps/details?id=com.arn.scrobble
[obtainium]: img/obtainium.svg
[obtainium-link]: https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/kawaiiDango/pano-scrobbler
[fdroid]: img/fdroid.svg
[fdroid-link]: https://kawaiidango.github.io/pano-scrobbler/fdroid/repo?fingerprint=9954ECAB27F9FCE8290AC75A33F3DFE9FE5F6F5B8E6F33AD7F98307AC4D487BA
[ko-fi]: img/ko-fi.svg
[ko-fi-link]: https://ko-fi.com/kawaiiDango
[crowdin]: img/crowdin.svg
[crowdin-link]: https://crowdin.com/project/pscrobbler

<img src="screenshots/scrobbles-desktop.jpg" alt="scrobbles screen" width="250"/> <img src="screenshots/charts-desktop.jpg" alt="charts screen" width="250"/>

<img src="screenshots/friends-mobile.jpg" alt="friends screen" width="150"/> <img src="screenshots/details-mobile.jpg" alt="details screen" width="150"/>

### Downloads

**Windows:**

[![github-x64](img/github-x64.svg)](https://github.com/kawaiiDango/pano-scrobbler/releases/latest/download/pano-scrobbler-windows-x64.exe)

**Linux:**

[![github-x64](img/github-x64.svg)](https://github.com/kawaiiDango/pano-scrobbler/releases/latest/download/pano-scrobbler-linux-x64.AppImage) [![github-arm64](img/github-arm64.svg)](https://github.com/kawaiiDango/pano-scrobbler/releases/latest/download/pano-scrobbler-liinux-arm64.AppImage) [![aur](img/aur.svg)](https://aur.archlinux.org/packages/pano-scrobbler-bin)

**Android (phones, tablets, TVs and Chromebooks):**

[![github](img/github.svg)](https://github.com/kawaiiDango/pano-scrobbler/releases) [![fdroid][fdroid]][fdroid-link] [![obtainium][obtainium]][obtainium-link] [![play-store][play-store]][play-store-link]

Since v4.0, the non-Play builds do not use Firebase Crashlytics, Google Play Services, or any closed source libraries.

The non-Play variant can optionally display a persistent notification on Android 14+.
This may help prevent the system from killing the app in the background on devices from certain manufacturers.

**APK SHA256 fingerprint for non-Play builds:**
```
9E:68:AB:6B:D6:11:CE:79:DD:5D:53:E3:5A:15:CF:93:CF:73:9A:4E:94:C4:9A:B1:BF:10:DD:12:65:F0:2E:6C
```

### Translate

[![translate][crowdin]][crowdin-link]

## [FAQ](faq.md) • [Privacy](privacy-policy.md) • [Compiling](instructions.md) • [TV](tv.md)

### Features:

#### For all platforms (Windows, Linux, Android, Android TV):

- No ads ever
- Scrobbles to Lastfm, Librefm, ListenBrainz, Pleroma and other compatible services
- View song, album, artist, album artist, and tag details
- View scrobbles from a specific time, such as last year or last month
- Extract or fix metadata such as "Remastered" with regex pattern edits
- Extract the first artist from a string of all performers before scrobbling
- Block artists, songs, etc., and automatically skip or mute when they play
- Check what your followed users are listening to and view their stats
- Import and export settings, edits, and blocklists
- View charts with change indicators for specific time periods,
- View scrobble count graphs and tag clouds
- Get a random song, album, or artist from your listening history
- Search Lastfm for a songs, artists, or albums
- Themes
- Remember and see apps you scrobbled from and play directly in them

#### For desktop and Android (except TV):
- Scrobble to a CSV or JSONL file locally
- Interactive notification - view song info, edit, love, cancel, or block songs directly from the
  notification
- Collage generator
- Add or remove personal tags from the info screen
- Edit or delete existing scrobbles. Remembers edits
- Control Pano Scrobbler from automation apps on Android or command-line on desktop

#### Android only (except TV):
- Scrobble from music recognition apps and Pixel Now Playing
- Charts as a customizable home-screen widget
- Get your top scrobbles digests as a notification at the end of every week, month and year

### Credits

- YouTube title parser from [Web Scrobbler](https://github.com/web-scrobbler/web-scrobbler) and [Metadata Filter](https://github.com/web-scrobbler/metadata-filter)
- Icons from [pictogrammers.com](https://pictogrammers.com) and [material.io](https://material.io)
- Genres filter from [everynoise.com](https://everynoise.com)
- Tidal SteelSeries Integration from [TidalRPC](https://github.com/BitesizedLion/TidalRPC)
- Artists list from [MusicBrainz](https://musicbrainz.org)

Thanks to the amazing [translators](composeApp/src/commonMain/composeResources/files/crowdin_members.txt) and everyone who
reported bugs and helped me with this project.

### Disclaimer

This project is not affiliated with Last.fm, or any other scrobbling service.

### License

Pano Scrobbler is licensed under the [GNU General Public License v3](http://www.gnu.org/copyleft/gpl.html).
