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
source ./installJabba.sh

# Install the requested JDK version:
jabba install $JDK_VERSION

# Update environment variables:
# 'jabba use' does not work on msys currently.
case "$OSTYPE" in
    msys)
    # Workaround for msys: Set JAVA_HOME and PATH manually.
    # We need to prepare the path returned by Jabba for this to work in msys.
    JABBA_JDK_HOME=$(jabba which --home $JDK_VERSION)
    JABBA_JDK_HOME=${JABBA_JDK_HOME//\\/\\\\}
    JABBA_JDK_HOME=$(cygpath -u $JABBA_JDK_HOME)
    JAVA_HOME=$JABBA_JDK_HOME
    PATH=$JABBA_JDK_HOME/bin:$PATH
    ;;
    *)
    jabba use $JDK_VERSION
    ;;
esac

# Verify that the expected Java version is used:
echo Java home: $JAVA_HOME
echo Path: $PATH
echo Active Java version: $(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
