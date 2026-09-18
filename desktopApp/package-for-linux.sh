#!/bin/bash

scriptDir="$(cd "$(dirname "$0")" && pwd)"
arch=$(uname -m)

if [ "$arch" = "aarch64" ]; then
    resourcesDirName="linux-arm64"
else
    resourcesDirName="linux-x64"
fi
appNameWithoutSpaces="pano-scrobbler"
nativeImageDir="$scriptDir/build/native/$resourcesDirName"
appDir="/tmp/PanoScrobbler.AppDir"
libExecDir="$appDir/usr/libexec/pano-scrobbler"
distDir="$scriptDir/../dist"

# Read version code from version.txt
verCode=$(cat "$scriptDir/../version.txt")
verName="$((verCode / 100)).$((verCode % 100))"

# Strip .so files
strip --strip-unneeded "$nativeImageDir"/*.so
strip --strip-unneeded "$nativeImageDir"/lib/*.so

# Clean AppDir
rm -rf "$appDir"

install -Dm644 -t "${libExecDir}/lib/" "${nativeImageDir}"/lib/*.so
install -Dm644 -t "${libExecDir}/" "${nativeImageDir}"/*.so
install -Dm644 -t "${libExecDir}/icons/hicolor/scalable/apps/" "${nativeImageDir}/icons/hicolor/scalable/apps/"*.svg
install -Dm644 -t "${libExecDir}/icons/hicolor/symbolic/apps/" "${nativeImageDir}/icons/hicolor/symbolic/apps/"*.svg
install -Dm644 -t "${libExecDir}/icons/hicolor/" "${nativeImageDir}/icons/hicolor/index.theme"
install -Dm644 -t "${libExecDir}/" "${nativeImageDir}"/LICENSE
install -Dm644 -t "${libExecDir}/" "${nativeImageDir}"/${appNameWithoutSpaces}.desktop
install -Dm755 -t "${libExecDir}/" "${nativeImageDir}/${appNameWithoutSpaces}"

# Create tarball
tarFile="$distDir/$appNameWithoutSpaces-$resourcesDirName.tar.zst"
ZSTD_CLEVEL=19 tar --zstd -cf "$tarFile" -C "$libExecDir" .

# Relauncher script for appimage
echo '#!/bin/bash
APP="$1"
sleep 3
"$APP" &' > "$libExecDir/relaunch.sh"
chmod +x "$libExecDir/relaunch.sh"

# LICENSE
install -d "$appDir/usr/share/licenses/$appNameWithoutSpaces"
mv "$libExecDir/LICENSE" "$appDir/usr/share/licenses/$appNameWithoutSpaces/"

# Icons
mv "$libExecDir/icons" "$appDir/usr/share/"

for f in "$appDir/usr/share/icons/hicolor/symbolic/apps/"*-symbolic.svg; do
  mv -- "$f" "${f%-symbolic.svg}-appimage-symbolic.svg"
done

for f in "$appDir/usr/share/icons/hicolor/scalable/apps/"*.svg; do
  mv -- "$f" "${f%.svg}-appimage.svg"
done

cp "$appDir/usr/share/icons/hicolor/scalable/apps/"*.svg "$appDir/"

# Desktop file
desktopFile="$libExecDir/$appNameWithoutSpaces.desktop"
sed -i -e "s/^Icon=.*/Icon=$appNameWithoutSpaces-appimage/" "$desktopFile"
sed -e "s/^Exec=.*/Exec=AppRun %U/" "$desktopFile" >  "$appDir/$appNameWithoutSpaces.desktop"
install -d "$appDir/usr/share/applications/"
mv "$desktopFile" "$appDir/usr/share/applications/"

# Create AppRun symlink
ln -srf "$libExecDir/$appNameWithoutSpaces" "$appDir/AppRun"

# Download appimagetool if missing
appImageToolFile="$HOME/appimagetool-$arch.AppImage"
if [ ! -f "$appImageToolFile" ]; then
    curl -L -o "$appImageToolFile" "https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-$arch.AppImage"
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

install -Dm644 -t "${debPkgDir}/opt/$appNameWithoutSpaces/lib/" "${nativeImageDir}"/lib/*.so
install -Dm644 -t "${debPkgDir}/opt/$appNameWithoutSpaces/" "${nativeImageDir}"/*.so
install -Dm755 -t "${debPkgDir}/opt/$appNameWithoutSpaces/" "${nativeImageDir}/${appNameWithoutSpaces}"
install -d "${debPkgDir}/usr/bin/"
ln -srf "${debPkgDir}/opt/$appNameWithoutSpaces/$appNameWithoutSpaces" "${debPkgDir}/usr/bin/${appNameWithoutSpaces}"

install -Dm644 "${nativeImageDir}/${appNameWithoutSpaces}.desktop" "${debPkgDir}/usr/share/applications/${appNameWithoutSpaces}.desktop"
install -Dm644 -t "${debPkgDir}/usr/share/icons/hicolor/scalable/apps/" "${nativeImageDir}/icons/hicolor/scalable/apps/"*.svg
install -Dm644 -t "${debPkgDir}/usr/share/icons/hicolor/symbolic/apps/" "${nativeImageDir}/icons/hicolor/symbolic/apps/"*.svg
install -Dm644 -t "${debPkgDir}/usr/share/licenses/${appNameWithoutSpaces}/" "${nativeImageDir}/LICENSE"

installedSize=$(du -sk "${debPkgDir}" | awk '{print $1}')

if [ "$arch" = "aarch64" ]; then
    debArch="arm64"
else
    debArch="amd64"
fi

install -d $debPkgDir/DEBIAN

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

dpkg-deb -Zzstd -z19 --build --root-owner-group "${debPkgDir}" "${debFile}"