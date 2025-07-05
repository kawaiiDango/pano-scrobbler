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

# Download the last 3 assets and create the changelogs
mkdir -p "metadata/${PACKAGE}/en-US/changelogs"
for ((i=0; i<${#ASSET_URLS[@]} && i<3; i++)); do
    curl -L --create-dirs -o "repo/${REPO}-${TAGS[$i]}.apk" "${ASSET_URLS[$i]}"
    touch -d "${DATES[$i]}" "repo/${REPO}-${TAGS[$i]}.apk"
    CHANGELOG=$(echo "$JSON" | jq -r ".[$i] | .body")
    echo -e "## ${TAGS[$i]}\n${CHANGELOG}" > "metadata/${PACKAGE}/en-US/changelogs/${TAGS[$i]}.txt"
done

# copy screenshots
mkdir -p "metadata/${PACKAGE}/en-US/phoneScreenshots"
cp ../../screenshots/scrobbles-mobile.jpg "metadata/${PACKAGE}/en-US/phoneScreenshots/1scrobbles.jpg"
cp ../../screenshots/friends-mobile.jpg "metadata/${PACKAGE}/en-US/phoneScreenshots/2friends.jpg"
cp ../../screenshots/charts-mobile.jpg "metadata/${PACKAGE}/en-US/phoneScreenshots/3charts.jpg"
cp ../../screenshots/random-mobile.jpg "metadata/${PACKAGE}/en-US/phoneScreenshots/4random.jpg"
cp ../../screenshots/details-mobile.jpg "metadata/${PACKAGE}/en-US/phoneScreenshots/5details.jpg"

# copy icon
cp ../../composeApp/app-icons/pano-scrobbler.png "metadata/${PACKAGE}/en-US/icon.png"

# Run fdroid update
fdroid update --create-metadata --use-date-from-apk

# Replace the default icon
cp ../../composeApp/app-icons/pano-scrobbler.png repo/icons/icon.png
