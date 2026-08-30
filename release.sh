#!/usr/bin/env bash
# Bumps the version, builds the plugin jar and publishes it as a GitHub release.
# Each run produces a brand-new versioned jar and a matching GitHub release.
#
# Usage:
#   ./release.sh           # bumps the patch version (0.5.0 -> 0.5.1)
#   ./release.sh 0.6.0     # uses the given explicit version
set -euo pipefail
cd "$(dirname "$0")"

# --- version logic -------------------------------------------------------
VERSION=$(grep -E '^version=' gradle.properties | head -1 | cut -d= -f2 | tr -d ' ')

if [ "$#" -ge 1 ]; then
    NEW_VERSION="$1"
else
    MAJOR="${VERSION%%.*}"
    REST="${VERSION#*.}"
    MINOR="${REST%%.*}"
    PATCH="${REST#*.}"
    NEW_VERSION="${MAJOR}.${MINOR}.$((PATCH + 1))"
fi

if ! echo "$NEW_VERSION" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'; then
    echo "Invalid version: $NEW_VERSION (expected x.y.z)" >&2
    exit 1
fi

# --- write version into gradle.properties --------------------------------
perl -pi -e "s/^version=.*/version=$NEW_VERSION/" gradle.properties
echo "Building version $NEW_VERSION"

# --- build ----------------------------------------------------------------
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew clean build

JAR="build/libs/PaperFilteredHoppers-${NEW_VERSION}.jar"
if [ ! -f "$JAR" ]; then
    echo "Expected jar not found: $JAR" >&2
    exit 1
fi
echo "Jar: $JAR"

# --- release --------------------------------------------------------------
git add -A
git commit -m "Release v${NEW_VERSION}"
git tag "v${NEW_VERSION}"
GIT_TERMINAL_PROMPT=0 git push origin main --tags
gh release create "v${NEW_VERSION}" "$JAR" --title "v${NEW_VERSION}" \
    --notes "Automated release v${NEW_VERSION}"
echo "Released v${NEW_VERSION}: https://github.com/Starf0x/PaperFilteredHoppers/releases/tag/v${NEW_VERSION}"