#!/bin/bash

# Base URL for the GitHub Pages site (no trailing slash)
BASE_URL="https://kawaiidango.github.io/pano-scrobbler"

# Hardcoded pages: "url_path|source_file"
# The source file is used to look up the last commit date via git log.
PAGES=(
  "/|README.md"
  "/faq.html|faq.md"
  "/privacy-policy.html|privacy-policy.md"
  "/instructions.html|instructions.md"
  "/fdroid/repo|bad-file-path.md"
)

get_lastmod() {
  local file="$1"
  # Use file mtime set by git restore-mtime in CI
  if [ -f "$file" ]; then
    date -r "$file" --iso-8601=seconds 2>/dev/null
  fi
}

echo '<?xml version="1.0" encoding="UTF-8"?>'
echo '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">'

for entry in "${PAGES[@]}"; do
  url_path="${entry%%|*}"
  file="${entry##*|}"

  lastmod="$(get_lastmod "$file")"

  echo "  <url>"
  echo "    <loc>${BASE_URL}${url_path}</loc>"
  # Skip lastmod if the file has no git history
  if [ -n "$lastmod" ]; then
    echo "    <lastmod>${lastmod}</lastmod>"
  fi
  echo "  </url>"
done

echo '</urlset>'