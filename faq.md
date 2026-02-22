# FAQ

### [android] The app skips scrobbles when it is in the background / The app says that it has been killed

Some manufacturers such as Xiaomi, Huawei, OnePlus and others do not allow 3rd party apps to run in
the background by default. Check [dontkillmyapp.com](https://dontkillmyapp.com) and follow all the
steps in there, for your device. The non-Play version of this app can optionally display a
persistent notification at all times, which may help. It may be impossible to use this app on a
stock ROM from certain manufacturers, due to the said restrictions.

### [android] The app says that scrobbling is disabled

Check if Pano Scrobbler has the permission to read notifications from other apps. Go to Settings >
Apps > Special app access > Notification read, reply and control > Pano Scrobbler. The exact
location and name of the setting will depend on your Android version and manufacturer. Note that
this is different from the standard "notifications" permission, which has nothing to do with reading
notifications from other apps.

### All of my scrobbles from Spotify are duplicated

This happens if you have more than one scrobbler connected/running. Disable or uninstall other
scrobblers. Disconnect your Spotify account from the last.fm website.

### How do I scrobble only the first artist instead of all the performers?

Go to Configure > Use the first artist only

### The app is scrobbling non music videos from YouTube

Unfortunately there is no way to detect whether a video is a music video, from just the video title
and the channel name, reported to the OS media controls. You may block certain channels from
scrobbling or turn off YouTube scrobbling entirely, from settings.

### I cannot edit scrobbles older than 14 days

It is not possible to edit scrobbles older than 14 days using this app, regardless of whether you
have a last.fm pro subscription, due to the method used for editing.

### Pictures of some artists are missing even though they have a picture on last.fm

Artist pictures are sourced from Spotify because the last.fm API does not provide them. Due to the
implementation, the artist's name must have an exact match on Spotify. Sometimes, language
differences between the name on last.fm vs Spotify cause this issue.

As a workaround, you can enable "Approximate search for artist images". Note that this might give
you wrong images for some artists. You can also choose your own mapping to Spotify pictures or
choose your own from gallery.

### Pictures of some tracks are missing in the charts screen

Tracks do not have images for themselves. They have album arts depending on the album or single they
are associated with. The last.fm API may not provide an album name for some tracks. In that case, no
image will be displayed.

### Certain tracks are being ignored

Last.FM has a spam filter, which filters out tracks/albums/artists with titles similar to
"Unknown", ".mp3", "Track_01" etc

### The scrobbler submits incomplete data / does not submit album artists

This can happen if your music player does not provide the complete metadata in its
`MediaSession` on Android, `SystemMediaTransportControls` on Windows or `MPRIS` on Linux.

Use a different music player, which does that.

### What is the maximum number of edits that can be saved?

There is no limit for regular edits and blocks. However, there can be at most 50 active or enabled
regex rules for performance reasons, because it has to try out every regex rule for every scrobble.

### How do I stream music on this app?

This is not a music streaming app. However, you can click the search and play button, to search and
play the selected song on your favourite music player.

### [android, tv] The search and play button always opens a particular music player

This can happen if you have set that particular music player as the default. To fix this, go to that
music player's app info and tap on "Clear defaults".

### [android] Where is the scrobble from mic feature?

In-app scrobble from mic has been removed, in favor of scrobbling from Shazam/AutoShazam, Audile,
Pixel's Now Playing or Ambient Music Mod.

### [android] Where are the Tasker intents?

Tasker intents have been replaced with ContentProvider query URIs and an allowlist, which are more
secure.

### [android] I cannot grant access to read notifications on Android 13 or higher

For apps downloaded from outside app stores on Android 13+, this setting is restricted. Go to App
info > 3 dot menu > Allow restricted settings. Make sure you've viewed the "Restricted Setting"
prompt for this app at least once before going to App Info.

### [android, tv] I cannot find any system setting grant access to read notifications on my device [FAQ-nc]

Getting Notification access to work on TVs on Android 8.1 and below is not very straightforward due
to Android's limitations, as Google didn't provide a UI to do so.

This may also work on devices that do not have a UI to enable notification access, such as WSA or
old versions of Android Go.

- Open this page on a computer.
  URL: [kawaiidango.github.io/pano-scrobbler/faq](https://kawaiidango.github.io/pano-scrobbler/faq)
- Enable Developer Options, if you haven't (Settings > About > (Tap on Build Number 5 times))
- Enable ADB/USB debugging (Settings > Developer Options)
- On your PC, download ADB, if you haven't:
  [developer.android.com/studio/releases/platform-tools](https://developer.android.com/studio/releases/platform-tools)
- Connect your device to your computer via USB or connect to the same Wi-Fi network and use ADB over
  Wi-Fi
- Authorize the ADB connection on your device, if prompted
- Run the following command in your terminal:
- On Android 8.1 or higher:
```
adb shell cmd notification allow_listener com.arn.scrobble/com.arn.scrobble.media.NLService
```
- On Android 8.0 and lower:
```
adb shell 'settings put secure enabled_notification_listeners $(settings get secure enabled_notification_listeners):com.arn.scrobble/com.arn.scrobble.media.NLService'
```
- Do not add any additional spaces
- Disable USB debugging on your device when done

### [desktop] How do I launch the app as minimized to tray?

Use the `--minimized` or `-m` command line argument.

### [desktop] How do I store the app's data in a different location?

Use the `--data-dir` or `-d` command line argument, followed by the path to the directory.

### [desktop] How do I correctly scrobble media playing on a web browser?

Browsers on Windows do not report the complete song metadata to the OS media controls. The situation
is better on Linux, but depends on the website correctly reporting the metadata in their
MediaSession. The same applies to browser-like apps (e.g. most Electron and WebView apps). Use
WebScrobbler for web browsers, instead.

### [desktop] The app does not show notifications on Windows

To show notifications on Windows, the app's ID must be registered with the OS and linked to its
start menu shortcut. This is done by the installer in install mode. So, it won't work if you
are using it portable mode or have removed the start menu shortcut.

### The login WebView does not load [FAQ-wv]

Check if you have the required dependencies installed

- Arch:
```
sudo pacman -S webkit2gtk-4.1
```
- Debian:
```
sudo apt install libwebkit2gtk-4.1-0
```
- Fedora:
```
sudo dnf install webkit2gtk4.1
```
- Windows: WebView2 provided by Microsoft Edge Chromium

- Android: Android System WebView

### How do I get a refund for a 'Support this app' payment made through Google Play? [FAQ-rp]

You can request a refund within a 48-hour window, from
the [Google Play website](https://support.google.com/googleplay/answer/15574897).

### [nonplay] How do I get a refund for a 'Support this app' payment made through Ko-Fi or BuyMeACoffee? [FAQ-rn]

For voluntary donations of arbitrary amounts made through Ko-Fi or BuyMeACoffee, there are no
refunds, as these are donations for supporting the development of this free and open source project,
and not purchases.