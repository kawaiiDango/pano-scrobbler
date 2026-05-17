#!/bin/bash

scriptDir="$(cd "$(dirname "$0")" && pwd)"
pkgbuildDir="$scriptDir/../pano-scrobbler-bin"
flakeDir="$scriptDir/../pano-scrobbler-flake"

# --- Re-launch inside Arch container if not Arch ---
if [[ -z "$(command -v makepkg)" ]]; then
  exec podman run --rm \
    -e IN_CONTAINER=1 \
    -e GITHUB_TOKEN="${GITHUB_TOKEN:-}" \
    -v "$scriptDir:/scripts:z" \
    -v "$(realpath "$pkgbuildDir"):/pkgbuild:z" \
    -v "$(realpath "$flakeDir"):/flake:z" \
    -w /scripts \
    docker.io/archlinux:base \
    bash -c "pacman -Sy --noconfirm jq ed tinyxxd nix git && bash /scripts/$(basename "$0")"
fi

# if inside container, set dirs to their mount paths. Don't do it if the host is Arch.
if [[ -n "${IN_CONTAINER:-}" ]]; then
  pkgbuildDir="/pkgbuild"
  flakeDir="/flake"
fi

# Convert a 64-char hex SHA-256 string to Nix SRI format: sha256-<base64>
hex_to_sri() {
  echo "sha256-$(echo "$1" | xxd -r -p | base64)"
}

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
    published_at="$(jq -r '.published_at // empty' <<<"$release_json")"

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
    runuser -u nobody -- makepkg -D "$pkgbuildDir" --printsrcinfo > "$pkgbuildDir/.SRCINFO"
else
    echo "PKGBUILD not found" >&2
fi

# Update flake.nix (tag, version, x86_64-linux hash, aarch64-linux hash)
if [ -f "$flakeDir/flake.nix" ]; then
    # tag/sha vars are set above; bail if they're empty (PKGBUILD block was skipped)
    if [[ -z "${tag:-}" || -z "${sha_x64:-}" || -z "${sha_arm64:-}" ]]; then
      echo "Error: release metadata not available, skipping flake.nix update" >&2
    else
      sri_x64="$(hex_to_sri "$sha_x64")"
      sri_arm64="$(hex_to_sri "$sha_arm64")"

      ed -s "$flakeDir/flake.nix" <<EOF
g/^ *tag = /s|.*|      tag = "$tag";|
g/^ *version = /s|.*|      version = "$verName";|
g/"x86_64-linux" = "sha256-/s|.*|        "x86_64-linux" = "$sri_x64";|
g/"aarch64-linux" = "sha256-/s|.*|        "aarch64-linux" = "$sri_arm64";|
w
q
EOF

      nix --extra-experimental-features 'nix-command flakes' flake update nixpkgs --flake "$flakeDir"

      echo -e "flake.nix updated:\ntag=$tag\nversion=$verName\nsri_x64=$sri_x64\nsri_arm64=$sri_arm64"
    fi
else
    echo "flake.nix not found" >&2
fi

# Update .spec file (_pkgver, Version, %changelog)
specFile="$scriptDir/pano-scrobbler.spec"
if [ -f "$specFile" ]; then
    if [[ -z "${tag:-}" || -z "${verName:-}" || -z "${published_at:-}" ]]; then
      echo "Error: release metadata not available, skipping .spec update" >&2
    else
      changelog_date="$(date -d "$published_at" "+%a %b %d %Y")"

      ed -s "$specFile" <<EOF
g/^%global _pkgver /s|.*|%global _pkgver $tag|
g/^Version:/s|.*|Version:        $verName|
g/^\* [A-Za-z]/s|^\* [A-Za-z][a-z]* [A-Za-z][a-z]* [0-9]* [0-9]* \(.*\) - .*|* $changelog_date \1 - $verName-1|
g/^- Update to /s|.*|- Update to $verName|
w
q
EOF
      echo -e ".spec updated:\n_pkgver=$tag\nVersion=$verName\nchangelog_date=$changelog_date"
    fi
else
    echo ".spec not found" >&2
fi