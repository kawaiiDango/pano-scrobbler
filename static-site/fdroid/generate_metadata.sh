#!/bin/bash

mkdir -p static-site/fdroid/metadata

# Define the output file
output_file="static-site/fdroid/metadata/com.arn.scrobble.yml"

# Extract the features from Readme.md
features=$(sed -n '/### Features:/,/### F-droid repo/p' Readme.md | sed '1d;$d')

# Create the .yml metadata file
cat <<EOL > $output_file
AuthorName: 'kawaiiDango'
Categories:
- Multimedia
IssueTracker: 'https://github.com/kawaiiDango/pano-scrobbler/issues'
Name: Pano Scrobbler
SourceCode: 'https://github.com/kawaiiDango/pano-scrobbler'
Summary: 'Feature packed music tracker with slick animations'
Description: |
$features
AntiFeatures:
  - UpstreamNonFree
  - NonFreeNet
WebSite: 'https://github.com/kawaiiDango/pano-scrobbler'
Translation: https://crowdin.com/project/pscrobbler
Donate: https://ko-fi.com/kawaiiDango
AuthorEmail: kawaiiDango@pm.me
License: GPL-3.0
EOL

echo "Metadata file generated at $output_file"