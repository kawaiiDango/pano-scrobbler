#!/bin/bash

scriptDir="$(cd "$(dirname "$0")" && pwd)"
arch=$(uname -m)

if [ "$arch" = "aarch64" ]; then
    resourcesDirName="linux-arm64"
else
    resourcesDirName="linux-x64"
fi
appNameWithoutSpaces="pano-scrobbler"
nativeImageDir="$scriptDir/composeApp/build/compose/native/$resourcesDirName"
appDir="/tmp/PanoScrobbler.AppDir"
libExecDir="$appDir/usr/libexec/pano-scrobbler"
distDir="$scriptDir/dist"

# Read version code from version.txt
verCode=$(cat "$(dirname "$0")/version.txt")
verName="$((verCode / 100)).$((verCode % 100))"

# Clean and create AppDir
rm -rf "$appDir"
mkdir -p "$libExecDir"

# Copy executable files to a dir in Linux filesystem
cp -ar "$nativeImageDir/." "$libExecDir"

# Fix permissions
chmod 644 "$libExecDir"/*.*
chmod 644 "$libExecDir"/lib/*.*
chmod 644 "$libExecDir/LICENSE"
chmod 755 "$libExecDir/$appNameWithoutSpaces"

# Create tarball
tarFile="$distDir/$appNameWithoutSpaces-$resourcesDirName.tar.gz"
tar -czf "$tarFile" -C "$libExecDir" .

# Relauncher script for appimage
echo '#!/bin/bash
APP="$1"
sleep 3
"$APP" &' > "$libExecDir/relaunch.sh"
chmod +x "$libExecDir/relaunch.sh"

# LICENSE
mkdir -p "$appDir/usr/share/licenses/$appNameWithoutSpaces"
mv "$libExecDir/LICENSE" "$appDir/usr/share/licenses/$appNameWithoutSpaces/"

# Icon
mkdir -p "$appDir/usr/share/icons/hicolor/scalable/apps"
mv "$libExecDir/$appNameWithoutSpaces.svg" "$appDir/"
cp "$appDir/$appNameWithoutSpaces.svg" "$appDir/usr/share/icons/hicolor/scalable/apps/"

# Desktop file
desktopFile="$libExecDir/$appNameWithoutSpaces.desktop"
sed -i 's/^Exec=.*/Exec=AppRun %U/' "$desktopFile"
sed -i "s/^Icon=.*/Icon=$appNameWithoutSpaces/" "$desktopFile"
mv "$desktopFile" "$appDir/"

# Create AppRun symlink
ln -srf "$libExecDir/$appNameWithoutSpaces" "$appDir/AppRun"

# Download appimagetool if missing
appImageToolFile="$HOME/appimagetool-$arch.AppImage"
if [ ! -f "$appImageToolFile" ]; then
    wget -O "$appImageToolFile" "https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-$arch.AppImage"
    chmod +x "$appImageToolFile"
fi

# Build AppImage
distFile="$distDir/$appNameWithoutSpaces-$resourcesDirName.AppImage"
ARCH=$arch VERSION="$verName" "$appImageToolFile" "$appDir" "$distFile"
