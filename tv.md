# Pano Scrobbler On Old TVs

Getting Notification access to work on TV on Android 8.1 and below is not very straightforward due
to Android's limitations, as Google didn't provide a UI to do so.

This may also work on devices that do not allow notification access, such as WSA or old versions of
Android Go.

- Open this page on a PC (URL: bit.ly/pano-tv)

- Enable Developer Options, if you haven't (Settings > About > (Tap on Build Number 5 times))

- Enable ADB/USB debugging (Settings > Developer Options)

- On your PC, download ADB, if you haven't
  already: [developer.android.com/studio/releases/platform-tools](https://developer.android.com/studio/releases/platform-tools)


- Open a terminal, navigate to the folder which contains adb and type

```
    adb connect  <ip address of your TV>
```

```
    adb shell
```

- On Android 8.1 or higher:

```
    cmd notification allow_listener com.arn.scrobble/com.arn.scrobble.media.NLService
```

- If the above command didn't work, or you are on Android 8.0 and below:

```
    n=$(settings get secure enabled_notification_listeners)
```

```
    settings put secure enabled_notification_listeners $n:com.arn.scrobble/com.arn.scrobble.media.NLService
```

- Do not add any additional spaces

- Remember to disable ADB on your device when done, for security reasons