#!/usr/bin/env bash
if [ $# -eq 0 ]; then
    echo "Missing argument(s)"
    exit 1
fi

# Check for predefined JDK version aliases:
case "$1" in
  8)
    JAVA_VERSION="adopt@1.8.0-292"
    ;;
  16)
    JAVA_VERSION="adopt@1.16.0-1"
    ;;
  *)
    JAVA_VERSION=$1
    ;;
esac

echo Installing Java JDK: $JAVA_VERSION

# We use Jabba to manage multiple JDK versions regardless of platform.
# Install Jabba, if it is not already installed.
./installJabba.sh

# Install the requested JDK version:
./jabba install $JAVA_VERSION

# Update environment variable:
jabba_jdk_location=$(./jabba which --home "$JAVA_VERSION")
echo ::set-env name=JAVA_HOME::$jabba_jdk_location
echo ::add-path::$jabba_jdk_location/bin
