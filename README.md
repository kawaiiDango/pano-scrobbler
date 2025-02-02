## <img src="composeApp/src/commonMain/composeResources/drawable/ic_launcher_with_bg.svg" alt="app icon" width="32"/> Pano Scrobbler

[play-store]: img/play-store.svg

[play-store-link]: https://play.google.com/store/apps/details?id=com.arn.scrobble

[github]: img/github.svg

[github-link]: https://github.com/kawaiiDango/pano-scrobbler/releases

[fdroid]: img/fdroid.svg

[fdroid-link]: https://kawaiidango.github.io/pano-scrobbler/fdroid/repo?fingerprint=9954ECAB27F9FCE8290AC75A33F3DFE9FE5F6F5B8E6F33AD7F98307AC4D487BA

[ko-fi]: img/ko-fi.svg

[ko-fi-link]: https://ko-fi.com/kawaiiDango

[crowdin]: img/crowdin.svg

[crowdin-link]: https://crowdin.com/project/pscrobbler

<img src="https://i.imgur.com/pgETfhc.png" alt="scrobbles" width="150"/> <img src="https://i.imgur.com/Q7yPi2z.png" alt="friends" width="150"/> <img src="https://i.imgur.com/MUhcyBw.png" alt="charts" width="150"/> <img src="https://i.imgur.com/aikbtGR.png" alt="random" width="150"/>

### Downloads

**Android (phones, tablets and TVs):**

[![play-store][play-store]][play-store-link] [![github][github]][github-link] [![fdroid][fdroid]][fdroid-link]

### Translate
[![translate][crowdin]][crowdin-link]

## [FAQ](faq.md) • [Privacy](privacy-policy.md) • [Compiling](instructions.md) • [TV](tv.md)

### Features:

- Supports phones, TVs, tablets, and Android desktops, including Windows 11
- No ads ever
- Scrobbles to Lastfm, Librefm, GNUfm, ListenBrainz, Maloja, Pleroma
- \* Scrobble to a CSV or JSONL file locally
- \* Interactive notification- view song info, edit, love, cancel, or block songs directly from the
  notification
- View song, album, artist, album artist, and tag details
- View scrobbles from a specific time, such as last year or last month
- Edit or delete existing scrobbles. Remembers edits
- Extract or Fix metadata such as "Remastered" with regex pattern edits
- Block artists, songs, etc., and automatically skip or mute when they play
- Check what your followed users are listening to and view their stats
- Identify and scrobble a song from the microphone
- \* Scrobble from the S app and Pixel Now Playing
- Import and export settings, edits, and blocklists
- View charts with change indicators for specific time periods,
- \* Charts are also available as a customizable home-screen widget
- \* Collage generator
- View scrobble count graphs and tag clouds
- Get a random song, album, or artist from your listening history
- Search Lastfm for a songs, artists, or albums
- Fetch albums and album artists from Lastfm, if missing, before scrobbling
- \* Get your top scrobbles digests as a notification at the end of every week and month
- Add or remove personal tags from the info screen
- Mix and match themes
- \* Broadcast Intents for automation apps
- Remember and see apps you scrobbled from and play directly in them

Features marked with * are not available on TV due to OS limitations.

### Credits
- Inspiration from Musixmatch's FloatingLyrics
- YouTube title parser from [Web Scrobbler](https://github.com/web-scrobbler/web-scrobbler) and [Metadata Filter](https://github.com/web-scrobbler/metadata-filter)
- Icons from [pictogrammers.com](https://pictogrammers.com) and [material.io](https://material.io)
- Genres filter from [everynoise.com](https://everynoise.com)

Thanks to the amazing [translators](composeApp/src/commonMain/composeResources/files/crowdin_members.txt) and everyone who
reported bugs and helped me with this project.

### Disclaimer
This project is not affiliated with Last.fm, or any other scrobbling service.

### License
Pano Scrobbler is licensed under the GNU General Public License v3 (GPL-3) (http://www.gnu.org/copyleft/gpl.html).

#### Exceptions
* You are FORBIDDEN to use image files listed below, containing the Pano Scrobbler icon, in any way (unless for displaying Pano Scrobbler itself).

  ```
  svg/ic_banner.svg
  svg/ic_launcher-for-import.svg
  svg/vd_noti.svg
  svg/vd_noti_err.svg
  composeApp/resources/linux/app_icon.png
  composeApp/resources/macos/app_icon.icns
  composeApp/resources/windows/app_icon.ico
  composeApp/src/androidMain/res/drawable/vd_banner_fg.xml
  composeApp/src/androidMain/res/drawable/vd_launcher_fg.xml
  composeApp/src/androidMain/res/drawable/vd_launcher_fg_splash.xml
  composeApp/src/androidMain/res/drawable/vd_noti.xml
  composeApp/src/androidMain/res/drawable/vd_noti_err.xml
  composeApp/src/androidMain/res/drawable/vd_noti_persistent.xml
  composeApp/src/commonMain/composeResources/drawable/ic_launcher_with_bg.svg
  composeApp/src/commonMain/composeResources/drawable/vd_launcher_fg.xml
  composeApp/src/commonMain/composeResources/drawable/vd_noti.xml
  composeApp/src/commonMain/composeResources/drawable/vd_noti_err.xml
  composeApp/src/commonMain/composeResources/drawable/vd_noti_persistent.xml
  ```

* For the project as a whole, you are FORBIDDEN to publish the builds compiled by you
  (including modified, e.g., rename app name "Pano Scrobbler" to something else)
  to any app store (IBNLT Google Play Store, F-Droid, Amazon Appstore etc.).
