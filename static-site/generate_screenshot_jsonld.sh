#!/usr/bin/env bash

BASE_URL="https://kawaiidango.github.io/pano-scrobbler/"
DIRS=(
  "./desktop-screenshots"
  "./fastlane/metadata/android/en-US/images/phoneScreenshots"
)

json='['
first=true

for dir in "${DIRS[@]}"; do
  for filepath in "$dir"/*; do
    [ -f "$filepath" ] || continue
    url="${BASE_URL}${dir#./}/$(basename "$filepath")"
    [ "$first" = true ] && first=false || json+=','
    json+="{\"@type\":\"ImageObject\",\"url\":\"${url}\"}"
  done
done

json+=']'

echo "$json"