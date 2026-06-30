#!/bin/bash

scriptDir="$(cd "$(dirname "$0")" && pwd)"
pkgbuildDir="$scriptDir/../pano-scrobbler-bin"
flakeDir="$scriptDir/../pano-scrobbler-flake"
netlifyDir="$scriptDir/../pano-scrobbler-flake/netlify-root"

if [[ ! -v NO_CONTAINER ]]; then
  exec podman run --rm \
    -e IN_CONTAINER=1 \
    -e GITHUB_TOKEN="${GITHUB_TOKEN:-}" \
    -v "$scriptDir:/scripts:z" \
    -v "$(realpath "$pkgbuildDir"):/pkgbuild:z" \
    -v "$(realpath "$flakeDir"):/flake:z" \
    -v "$(realpath "$netlifyDir"):/netlify-root:z" \
    -w /scripts \
    -v "/run/user/$(id -u)/gnupg/S.gpg-agent:/root/.gnupg/S.gpg-agent" \
    -v ~/.gnupg/pubring.kbx:/root/.gnupg/pubring.kbx:ro \
    docker.io/archlinux:base \
    bash -c "pacman -Sy --noconfirm jq ed tinyxxd nix git dpkg && bash /scripts/$(basename "$0")"
fi

# if inside container, set dirs to their mount paths. Don't do it if the host is Arch.
if [[ -n "${IN_CONTAINER:-}" ]]; then
  pkgbuildDir="/pkgbuild"
  flakeDir="/flake"
  netlifyDir="/netlify-root"
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
specFile="$flakeDir/pano-scrobbler.spec"
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

# Update apt repo

PKG_NAME=$REPO
COMPONENT="main"
REPO_DIR="${netlifyDir}/apt"
POOL_PATH="pool/$COMPONENT/p/${PKG_NAME}"
DIST="all"

GPG_KEY_ID="5CB896FA8DAF303AEB5FCE4DDB06725ACB6921A0"

GITHUB_RELEASE_BASE="https://github.com/kawaiiDango/${PKG_NAME}/releases/download/${tag}"
REDIRECTS="${netlifyDir}/_redirects"

TMPDIR="/tmp/pano-scrobbler-deb-files"
trap 'rm -rf "${TMPDIR}"' EXIT
mkdir -p "${TMPDIR}"
rm -f "${REDIRECTS}"

# Map APT arch to GitHub asset arch
declare -A GH_ARCH=(
    [amd64]="x64"
    [arm64]="arm64"
)

for ARCH in amd64 arm64; do
    ASSET="pano-scrobbler-linux-${GH_ARCH[$ARCH]}.deb"
    URL="${GITHUB_RELEASE_BASE}/${ASSET}"
    LOCAL_DEB="${TMPDIR}/${ASSET}"

    curl -fL --progress-bar -o "${LOCAL_DEB}" "${URL}"

    SIZE=$(stat -c%s "${LOCAL_DEB}")
    SHA256=$(sha256sum "${LOCAL_DEB}" | awk '{print $1}')

    # dpkg-deb --field emits all control fields, ready for a Packages file
    CONTROL_FIELDS=$(dpkg-deb --field "${LOCAL_DEB}")

    PACKAGES_ENTRY=$(printf '%s\nFilename: %s/%s\nSize: %s\nSHA256: %s\n' \
        "${CONTROL_FIELDS}" \
        "${POOL_PATH}" "${ASSET}" \
        "${SIZE}" "${SHA256}")

    echo "    Size: ${SIZE} B  SHA256: ${SHA256}"

    # Write Packages files and Update _redirects

    # APT-conventional filename advertised in the Packages index
    APT_FNAME="pano-scrobbler_${verName}_${ARCH}.deb"

    PKG_FILE="${REPO_DIR}/dists/${DIST}/${COMPONENT}/binary-${ARCH}/Packages"
    printf '%s\nFilename: %s/%s\nSize: %s\nSHA256: %s\n\n' \
        "${PACKAGES_ENTRY}" \
        "${POOL_PATH}" "${APT_FNAME}" \
        "${SIZE}" "${SHA256}" \
        > "${PKG_FILE}"
    # gzip -cn < "${PKG_FILE}" > "${PKG_FILE}.gz"

    # Write _redirects
    printf '/%s/%s  %s/%s  302\n' \
        "apt/pano-scrobbler/${POOL_PATH}" "${APT_FNAME}" \
        "${GITHUB_RELEASE_BASE}" "${ASSET}" >> "${REDIRECTS}"
done

# Generate Release file
RELEASE_FILE="${REPO_DIR}/dists/${DIST}/Release"
DATE=$(date -u "+%a, %d %b %Y %H:%M:%S UTC")

cat > "${RELEASE_FILE}" <<EOF
Origin: ${PKG_NAME}
Label: ${PKG_NAME}
Suite: ${DIST}
Codename: ${DIST}
Date: ${DATE}
Architectures: amd64 arm64
Components: ${COMPONENT}
Description: ${PKG_NAME} APT repository
EOF

# Helper: append a hash section
append_hashes() {
    local header="$1" hash_cmd="$2"
    echo "${header}:" >> "${RELEASE_FILE}"
    for ARCH in amd64 arm64; do
        # for FNAME in Packages Packages.gz; do
            local FNAME="Packages"
            local FILE="${REPO_DIR}/dists/${DIST}/${COMPONENT}/binary-${ARCH}/${FNAME}"
            local HASH SIZE
            HASH=$( ${hash_cmd} "${FILE}" | awk '{print $1}' )
            SIZE=$(stat -c%s "${FILE}")
            printf ' %s %s %s\n' "${HASH}" "${SIZE}" \
                "${COMPONENT}/binary-${ARCH}/${FNAME}" >> "${RELEASE_FILE}"
        # done
    done
}

append_hashes "SHA256" "sha256sum"

# Clear-signed (preferred by modern apt)
gpg --yes --default-key "${GPG_KEY_ID}" \
    --armor --clearsign \
    --output "${REPO_DIR}/dists/${DIST}/InRelease" \
    "${RELEASE_FILE}"

rm "${RELEASE_FILE}"