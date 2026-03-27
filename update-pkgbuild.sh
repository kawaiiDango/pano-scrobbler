#!/bin/bash

scriptDir="$(cd "$(dirname "$0")" && pwd)"
pkgbuildDir="$scriptDir/../pano-scrobbler-bin"

# --- Re-launch inside Arch container if not Arch ---
if [[ -z "$(command -v makepkg)" ]]; then
  exec podman run --rm \
    -e IN_CONTAINER=1 \
    -e GITHUB_TOKEN="${GITHUB_TOKEN:-}" \
    -v "$scriptDir:/scripts:z" \
    -v "$(realpath "$pkgbuildDir"):/pkgbuild:z" \
    -w /scripts \
    docker.io/archlinux:base \
    bash -c "pacman -Sy --noconfirm jq ed && useradd -u $(id -u) -m dango && su dango -c 'bash /scripts/$(basename "$0")'"
fi

# if inside container, set pkgbuildDir to /pkgbuild. Don't do it if the host is Arch.
if [[ -n "${IN_CONTAINER:-}" ]]; then  
  pkgbuildDir="/pkgbuild"
fi

# Update PKGBUILD (_pkgver, pkgver, sha256sums)
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

    ed -s "$pkgbuildDir/PKGBUILD" <<EOF
g/^_pkgver=/s|.*|_pkgver=$tag|
g/^pkgver=/s|.*|pkgver=$verName|
g/^pkgrel=/s|.*|pkgrel=1|
g/^sha256sums_x86_64=(/s|.*|sha256sums_x86_64=('$sha_x64')|
g/^sha256sums_aarch64=(/s|.*|sha256sums_aarch64=('$sha_arm64')|
w
q
EOF
    echo -e "PKGBUILD updated:\n_pkgver=$tag\npkgver=$verName\nsha_x64=$sha_x64\nsha_arm64=$sha_arm64"
    makepkg -D "$pkgbuildDir" --printsrcinfo > "$pkgbuildDir/.SRCINFO"
else
    echo "PKGBUILD not found" >&2
fi