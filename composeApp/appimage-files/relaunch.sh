#!/bin/bash
APP="$1"
sleep 3     # Wait for main process to exit and file to be released
"$APP" &