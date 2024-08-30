# Pano Scrobbler FAQ

### The app skips scrobbles when it is in the background / The app says that it has been killed

Some manufacturers impose drastic background restrictions on 3rd party apps. Try disabling battery optimisations for this app. Sometimes, that step may not be enough. Please check [dontkillmyapp.com](https://dontkillmyapp.com) to see if there are any solutions for your device. It may be impossible to use this app on a stock ROM from certain manufacturers, due to the said restrictions.


### All of my scrobbles from Spotify are duplicated

This happens if you have more than one scrobbler running. Disable or uninstall other scrobblers. Disconnect your Spotify account from the last.fm website.


### The app is scrobbling non music videos from YouTube

Unfortunately there is no way to detect whether a video is a music video, from the metadata provided by YouTube on Android. You may block certain channels from scrobbling or turn off YouTube scrobbling from settings.


### I cannot edit scrobbles older than 14 days

It is not possible to edit scrobbles older than 14 days using this app, whether or not you have a last.fm pro subscription due to the method used for editing.


### Pictures of some artists are missing even though they have a picture on last.fm

Artist pictures are sourced from Spotify because the last.fm API does not provide them. Due to the implementation, the artist's name must have an exact match on Spotify. Sometimes, language differences between the name on last.fm vs Spotify cause this issue.

As a workaround, you can enable "Approximate search for artist images". Note that this might give you wrong images for some artists. You can also choose your own mapping to Spotify pictures or choose your own from gallery.


### Certain tracks are being ignored

Last.FM has a spam filter, which filters out tracks/albums/artists with titles similar to "Unknown" or ".mp3" or "Track_01".


### The scrobbler does not seem to pick up album artists

This can happen if your music player does not provide the album artist metadata to other apps like this scrobbler. Use a different music player, which does that.


### How do I stream music on this app?

This is not a music streaming app. However, you can click the search and play button, to search and play the selected song on your favourite music player.


### The search and play button always opens a particular music player

This can happen if you have set that particular music player as the default. To fix this, go to that music player's app info and tap on "Clear defaults".