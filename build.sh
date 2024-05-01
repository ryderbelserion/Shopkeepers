#!/usr/bin/env bash
# Abort on error
set -e

pushd "$(dirname "$BASH_SOURCE")"
trap popd EXIT

BUILD_TYPE="full"
# Optional argument 'minimal': Only builds the api and main projects.
if [ $# -eq 1 ] && [ "$1" = "minimal" ]; then
    echo "Minimal build: Only building api and main projects."
    BUILD_TYPE="minimal"
fi

# Build and install the required Spigot dependencies:
./scripts/installSpigotDependencies.sh $BUILD_TYPE

# We require Java 17 to build:
source scripts/installJDK.sh 17

# Build via Gradle:
if [ "$BUILD_TYPE" = "minimal" ]; then
    ./gradlew cleanInstallMinimal
else
    ./gradlew cleanInstall
fi
