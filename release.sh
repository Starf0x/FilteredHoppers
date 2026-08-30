#!/usr/bin/env bash
# Builds the plugin jar and publishes it as a GitHub release. A plain rebuild
# already bumps to a newer version (see build.gradle.kts); this script builds
# and uploads the resulting version to GitHub.
#
# Usage:
#   ./release.sh           # bumps the patch version (0.5.1 -> 0.5.2) and releases
#   ./release.sh 0.6.0     # release a specific version instead
set -euo pipefail
cd "$(dirname "$0")"

# --- build ----------------------------------------------------------------
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
export PATH="$JAVA_HOME/bin:$PATH"

if [ "$#" -ge 1 ]; then
    VERSION="$1"
    if ! echo "$VERSION" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'; then
        echo "Invalid version: $VERSION (expected x.y.z)" >&2
        exit 1
    fi
    echo "Building version $VERSION"
    ./gradlew clean build -PnextVersion="$VERSION"
else
    echo "Building next version"
    ./gradlew clean build
fi

NEW_VERSION=$(grep -E '^version=' gradle.properties | head -1 | cut -d= -f2 | tr -d ' ')
echo "Version: $NEW_VERSION"

JAR="build/libs/FilteredHoppers-${NEW_VERSION}.jar"
if [ ! -f "$JAR" ]; then
    echo "Expected jar not found: $JAR" >&2
    exit 1
fi
echo "Jar: $JAR"

# --- release ---------------------------------------------------------------
git add -A
git commit -m "Release v${NEW_VERSION}"
git tag "v${NEW_VERSION}"
GIT_TERMINAL_PROMPT=0 git push origin main --tags
gh release create "v${NEW_VERSION}" "$JAR" --title "v${NEW_VERSION}" \
    --notes "Automated release v${NEW_VERSION}"
echo "Released v${NEW_VERSION}: https://github.com/Starf0x/FilteredHoppers/releases/tag/v${NEW_VERSION}"