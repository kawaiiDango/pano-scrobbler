# Changelog v3 to v4

- The app has been rewritten from ground up using Compose Multiplatform
- Now available for **Android, Windows and Linux**
- Uses Material 3 Expressive
- The option to display a persistent notification on Android 14+ is back for non Play Store builds. This may help prevent the system from killing the app in the background on some devices
- The builds provided on Github no longer contain non-free dependencies
- Play Store purchases will no longer be available in the Github builds, which is in line with the Play Store policies
- Tasker intents have been replaced with ContentProvider query URIs and an allowlist, which are more secure
- In-app scrobble from mic has been removed, in favour of using Shazam/AutoShazam
- Native Maloja support has been removed, in favour of using their ListenBrainz API
- Regex edits logic has changed. Now supports blocking scrobbles
- Pending scrobbles now show their last attempt timestamp and why they failed
- Targets Android 16, dropped support for Android 6

- Implemented auto downloading of updates from Github releases for desktop
- Fixed scrobble edits not being applied
- Fixed some UI bugs
- Fixed a segfault on Linux

- Implemented QR code login for last.fm and libre.fm on TV.
- Editing and deleting scrobbles is no longer available on TV
- Removed some features from TV that required typing on TV
- Now scrobbles Apple Music (Windows, Android), Cider (Linux) and Spotify (Android) with the first artist and the correct album artist, by fetching them from the iTunes and Spotify API
- Import/export over the network is now available on desktop
- Added rounded borders to generated collages
- Now continues processing all regex rules even if a match was found
- Fixed pending scrobbles getting deleted at start
- Regex tester will now highlight the required text fields
- Reworked the now playing animation
- Added a button to not save an edited scrobble
- Fixed a segfault on desktop
- Use xdg-open on Linux to open links, when the default method fails
- Using heart button to like on the media player now also supports Apple Music, Tidal, Oto music, Pi Music, Symfonium, Echo and Metrolist
- Added support for the music recognition app, Audile

- UI tweaks for the Android widget
- Reworked scrobbling from music recognition apps for Android

- Added a per-player option to scrobble the first artist only
- Fixed duplicate scrobbles from Pixel Now Playing
- The desktop window now has a finite minimum size
- Other bug fixes
- Translation updates by the translators on Crowdin