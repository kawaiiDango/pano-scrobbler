#!/bin/bash

USER=kawaiiDango
REPO=pano-scrobbler
PACKAGE=com.arn.scrobble
# Define the URL of the GitHub repository
REPO_URL="https://api.github.com/repos/$USER/$REPO/releases"

# Check if jq package is installed, and install it if necessary
if ! command -v jq &> /dev/null; then
    sudo apt-get install -y jq
fi

# Use curl to get the JSON response for all releases
JSON=$(curl --silent "$REPO_URL")

# Extract the URLs of the assets named "pano-scrobbler-release.apk" and the corresponding tags
ASSET_URLS=$(echo "$JSON" | jq -r '.[] | select(.assets[0].name | endswith(".apk")) | .assets[0].browser_download_url')
TAGS=$(echo "$JSON" | jq -r '.[] | select(.assets[0].name | endswith(".apk")) | .tag_name')

# Convert the URLs and tags to arrays
ASSET_URLS=($ASSET_URLS)
TAGS=($TAGS)

# Download the last 5 assets
for ((i=0; i<${#ASSET_URLS[@]} && i<5; i++)); do
    curl -L --create-dirs -o "repo/${REPO}-${TAGS[$i]}.apk" "${ASSET_URLS[$i]}"
done

# download screenshots
curl -L --create-dirs -o "repo/${PACKAGE}/en-US/phoneScreenshots/1scrobbles.png" "https://i.imgur.com/pgETfhc.png"
curl -L -o "repo/${PACKAGE}/en-US/phoneScreenshots/2friends.png" "https://i.imgur.com/Q7yPi2z.png"
curl -L -o "repo/${PACKAGE}/en-US/phoneScreenshots/3charts.png" "https://i.imgur.com/MUhcyBw.png"
curl -L -o "repo/${PACKAGE}/en-US/phoneScreenshots/4random.png" "https://i.imgur.com/aikbtGR.png"

# copy icon
cp ../../app/src/main/play/listings/en-US/icon/icon.png "repo/${PACKAGE}/en-US/icon.png"

# Run fdroid update
fdroid update --create-metadata