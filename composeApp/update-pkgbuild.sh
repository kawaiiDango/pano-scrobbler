scriptDir="$(cd "$(dirname "$0")" && pwd)"

# Update PKGBUILD (_pkgver, pkgver, sha256sums)
pkgbuildDir="$scriptDir/../../pano-scrobbler-bin"
if [ -f "$pkgbuildDir/PKGBUILD" ]; then
    # Read version code from version.txt
    verCode=$(cat "$(dirname "$0")/version.txt")
    verName="$((verCode / 100)).$((verCode % 100))"

    # Generate sha256sums
    sha256_x64=$(sha256sum "$scriptDir/dist/pano-scrobbler-linux-x64.tar.gz" | awk '{print $1}')
    sha256_arm64=$(sha256sum "$scriptDir/dist/pano-scrobbler-linux-arm64.tar.gz" | awk '{print $1}')

    sed -i "s/^_pkgver=.*/_pkgver=$verCode/" "$pkgbuildDir/PKGBUILD"
    sed -i "s/^pkgver=.*/pkgver=$verName/" "$pkgbuildDir/PKGBUILD"
    sed -i "s/^sha256sums_x86_64=(.*/sha256sums_x86_64=('$sha256_x64')/" "$pkgbuildDir/PKGBUILD"
    sed -i "s/^sha256sums_aarch64=(.*/sha256sums_aarch64=('$sha256_arm64')/" "$pkgbuildDir/PKGBUILD"
    echo "PKGBUILD updated: _pkgver=$verCode, pkgver=$verName, sha256_x64=$sha256_x64, sha256_arm64=$sha256_arm64"
    makepkg -D "$pkgbuildDir" --printsrcinfo > "$pkgbuildDir/.SRCINFO"
else
    echo "PKGBUILD not found" >&2
fi