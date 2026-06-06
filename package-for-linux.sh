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

# Strip .so files
strip --strip-unneeded "$libExecDir"/*.so
strip --strip-unneeded "$libExecDir"/lib/*.so

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
sed -i "s/^Exec=.*/Exec=AppRun %U/" "$desktopFile"
sed -i "s/^Icon=.*/Icon=$appNameWithoutSpaces/" "$desktopFile"
cp "$desktopFile" "$appDir/"

# Again
sed -i "s/^Exec=.*/Exec=$appNameWithoutSpaces %U/" "$desktopFile"
sed -i "s/^Icon=.*/Icon=$appNameWithoutSpaces/" "$desktopFile"
mkdir -p "$appDir/usr/share/applications/"
mv "$desktopFile" "$appDir/usr/share/applications/"

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

# Build deb package, if dpkg-deb is available
if ! command -v dpkg-deb &> /dev/null; then
    echo "dpkg-deb could not be found, skipping deb package creation."
    exit 0
fi

debFile="${distDir}/${appNameWithoutSpaces}-${resourcesDirName}.deb"
debPkgDir="/tmp/pano-scrobbler-deb"

rm -rf "$debPkgDir"

mkdir -p \
  $debPkgDir/usr/{bin,opt/pano-scrobbler/lib,share/{applications,licenses/pano-scrobbler,icons/hicolor/scalable/apps}} \
  $debPkgDir/DEBIAN

install -m644 "${nativeImageDir}"/*.so        "${debPkgDir}/usr/opt/$appNameWithoutSpaces/"
install -m644 "${nativeImageDir}"/lib/*.so    "${debPkgDir}/usr/opt/$appNameWithoutSpaces/lib/"
install -m755 "${nativeImageDir}/${appNameWithoutSpaces}" "${debPkgDir}/usr/opt/$appNameWithoutSpaces"
ln -srf "${debPkgDir}/usr/opt/$appNameWithoutSpaces/$appNameWithoutSpaces" "${debPkgDir}/usr/bin/${appNameWithoutSpaces}"

desktopDst="${debPkgDir}/usr/share/applications/${appNameWithoutSpaces}.desktop"
sed \
    -e 's|^Exec=.*|Exec=pano-scrobbler %U|' \
    -e 's|^Icon=.*|Icon=pano-scrobbler|' \
    "${nativeImageDir}/${appNameWithoutSpaces}.desktop" > "${desktopDst}"
chmod 644 "${desktopDst}"

install -m644 "${nativeImageDir}/${appNameWithoutSpaces}.svg" \
    "${debPkgDir}/usr/share/icons/hicolor/scalable/apps/${appNameWithoutSpaces}.svg"
install -m644 "${nativeImageDir}/LICENSE" \
    "${debPkgDir}/usr/share/licenses/${appNameWithoutSpaces}/LICENSE"

installedSize=$(du -sk "${debPkgDir}" | awk '{print $1}')

if [ "$arch" = "aarch64" ]; then
    debArch="arm64"
else
    debArch="amd64"
fi

cat > "${debPkgDir}/DEBIAN/control" <<EOF
Package: ${appNameWithoutSpaces}
Version: ${verName}
Architecture: ${debArch}
Maintainer: kawaiiDango <kawaiiDango@protonmail.com>
Installed-Size: ${installedSize}
Depends: dbus, libwebkitgtk-6.0-4
Section: sound
Priority: optional
Homepage: https://github.com/kawaiiDango/pano-scrobbler
Description: Feature packed cross-platform music tracker
EOF

dpkg-deb -Zzstd -z22 --build --root-owner-group "${debPkgDir}" "${debFile}"