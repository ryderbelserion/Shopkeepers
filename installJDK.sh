#!/usr/bin/env bash
if [ $# -eq 0 ]; then
    echo "Missing argument(s)"
    exit 1
fi

# Check for predefined JDK version aliases:
case "$1" in
  8)
    JDK_VERSION="adopt@1.8.0-292"
    ;;
  16)
    JDK_VERSION="adopt@1.16.0-1"
    ;;
  *)
    JDK_VERSION=$1
    ;;
esac

echo Installing Java JDK: $JDK_VERSION

# We use Jabba to manage multiple JDK versions regardless of platform.
# Install Jabba, if it is not already installed.
./installJabba.sh

# Install the requested JDK version:
./jabba install $JDK_VERSION

# Update environment variables:
# TODO This has no effect on Windows currently.
./jabba use $JDK_VERSION

# Verify that the expected Java version is used:
echo Java home: $JAVA_HOME
echo Path: $PATH
java --version
