#!/bin/bash

USER=kawaiiDango
REPO=pano-scrobbler
PACKAGE=com.arn.scrobble
# Define the URL of the GitHub repository
REPO_URL="https://api.github.com/repos/$USER/$REPO/releases"

# Use curl to get the JSON response for all releases
JSON=$(curl --silent "$REPO_URL")

# Extract the URLs of the assets named "pano-scrobbler-release.apk", the corresponding tags, the created_at dates
ASSET_URLS=$(echo "$JSON" | jq -r '.[] | select(.assets[0].name | endswith(".apk")) | .assets[0].browser_download_url')
TAGS=$(echo "$JSON" | jq -r '.[] | select(.assets[0].name | endswith(".apk")) | .tag_name')
DATES=$(echo "$JSON" | jq -r '.[] | select(.assets[0].name | endswith(".apk")) | .created_at')

# Convert the URLs, tags, dates to arrays
ASSET_URLS=($ASSET_URLS)
TAGS=($TAGS)
DATES=($DATES)

# Download the last 5 assets and create the changelogs
mkdir -p "metadata/${PACKAGE}/en-US/changelogs"
for ((i=0; i<${#ASSET_URLS[@]} && i<5; i++)); do
    curl -L --create-dirs -o "repo/${REPO}-${TAGS[$i]}.apk" "${ASSET_URLS[$i]}"
    touch -d "${DATES[$i]}" "repo/${REPO}-${TAGS[$i]}.apk"
    CHANGELOG=$(echo "$JSON" | jq -r ".[$i] | .body")
    echo -e "## ${TAGS[$i]}\n${CHANGELOG}" > "metadata/${PACKAGE}/en-US/changelogs/${TAGS[$i]}.txt"
done

# download screenshots
curl -L --create-dirs -o "metadata/${PACKAGE}/en-US/phoneScreenshots/1scrobbles.png" "https://i.imgur.com/pgETfhc.png"
curl -L -o "metadata/${PACKAGE}/en-US/phoneScreenshots/2friends.png" "https://i.imgur.com/Q7yPi2z.png"
curl -L -o "metadata/${PACKAGE}/en-US/phoneScreenshots/3charts.png" "https://i.imgur.com/MUhcyBw.png"
curl -L -o "metadata/${PACKAGE}/en-US/phoneScreenshots/4random.png" "https://i.imgur.com/aikbtGR.png"

# copy icon
cp ../../composeApp/icons/ic_launcher_with_bg.png "metadata/${PACKAGE}/en-US/icon.png"

# Run fdroid update
fdroid update --create-metadata --use-date-from-apk

# Replace the default icon
cp ../../composeApp/icons/ic_launcher_with_bg.png repo/icons/icon.png
