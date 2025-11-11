scriptDir="$(cd "$(dirname "$0")" && pwd)"

# Update PKGBUILD (_pkgver, pkgver, sha256sums)
pkgbuildDir="$scriptDir/../pano-scrobbler-bin"
if [ -f "$pkgbuildDir/PKGBUILD" ]; then
    OWNER="kawaiiDango"
    REPO="pano-scrobbler"

    ASSET_X64="pano-scrobbler-linux-x64.tar.gz"
    ASSET_ARM64="pano-scrobbler-linux-arm64.tar.gz"

    API="https://api.github.com"
    RELEASES_URL="$API/repos/$OWNER/$REPO/releases/latest"

    if ! command -v jq >/dev/null 2>&1; then
      echo "Error: jq is required but not found in PATH" >&2
      exit 1
    fi

    CURL_ARGS=(-fsSL)
    AUTH_HEADER=()
    if [[ -n "${GITHUB_TOKEN:-}" ]]; then
      AUTH_HEADER=(-H "Authorization: Bearer ${GITHUB_TOKEN}")
    fi

    release_json="$(
      curl "${CURL_ARGS[@]}" \
        -H "Accept: application/vnd.github+json" \
        -H "X-GitHub-Api-Version: 2022-11-28" \
        "${AUTH_HEADER[@]}" \
        "$RELEASES_URL"
    )"

    tag="$(jq -r '.tag_name // empty' <<<"$release_json")"
    if [[ -z "$tag" ]]; then
      echo "Error: could not determine latest tag for $OWNER/$REPO" >&2
      exit 1
    fi

    get_sha256_for_asset() {
      local name="$1"
      local digest
      digest="$(jq -r --arg NAME "$name" '
        (.assets[] | select(.name == $NAME) | .digest) // empty
      ' <<<"$release_json")"

      if [[ -z "$digest" ]]; then
        echo "Error: digest not found in API response for asset: $name" >&2
        return 1
      fi

      # Expect format "sha256:<64-hex>"
      local sha="${digest#sha256:}"
      if [[ ! "$sha" =~ ^[0-9a-fA-F]{64}$ ]]; then
        echo "Error: invalid sha256 digest format for $name: $digest" >&2
        return 1
      fi

      # Normalize to lowercase
      echo "${sha,,}"
    }

    verName="$((tag / 100)).$((tag % 100))"

    sha_x64="$(get_sha256_for_asset "$ASSET_X64")"
    sha_arm64="$(get_sha256_for_asset "$ASSET_ARM64")"

    sed -i "s/^_pkgver=.*/_pkgver=$tag/" "$pkgbuildDir/PKGBUILD"
    sed -i "s/^pkgver=.*/pkgver=$verName/" "$pkgbuildDir/PKGBUILD"
    sed -i "s/^sha256sums_x86_64=(.*/sha256sums_x86_64=('$sha_x64')/" "$pkgbuildDir/PKGBUILD"
    sed -i "s/^sha256sums_aarch64=(.*/sha256sums_aarch64=('$sha_arm64')/" "$pkgbuildDir/PKGBUILD"
    echo "PKGBUILD updated: _pkgver=$tag, pkgver=$verName, sha_x64=$sha_x64, sha_arm64=$sha_arm64"
    makepkg -D "$pkgbuildDir" --printsrcinfo > "$pkgbuildDir/.SRCINFO"
else
    echo "PKGBUILD not found" >&2
fi