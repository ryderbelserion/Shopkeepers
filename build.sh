#!/usr/bin/env bash
# Abort on error
set -e

pushd "$(dirname "$BASH_SOURCE")"
trap popd EXIT

# Build and install the required Spigot dependencies:
./scripts/installSpigotDependencies.sh

# We require Java 16 to build:
source scripts/installJDK.sh 16

# Build via Gradle:
./gradlew cleanInstall
