#!/usr/bin/env bash

# Skip if Jabba is already installed:
if [[ -f "jabba" ]]; then
  JABBA_VERSION=$(./jabba --version)
  echo Jabba is already installed: $JABBA_VERSION
  exit 0
fi

# Install script does not support msys (MinGW). See https://github.com/shyiko/jabba/issues/535
# But using the Jabba binary directly works as well.

JABBA_VERSION=0.11.2
echo Installing Jabba version ${JABBA_VERSION} ...

echo OSTYPE: $OSTYPE
case "$OSTYPE" in
  linux*)
    BINARY_URL=https://github.com/shyiko/jabba/releases/download/${JABBA_VERSION}/jabba-${JABBA_VERSION}-linux-amd64
    ;;
  darwin*)
    BINARY_URL=https://github.com/shyiko/jabba/releases/download/${JABBA_VERSION}/jabba-${JABBA_VERSION}-darwin-amd64
    ;;
  msys)
    BINARY_URL=https://github.com/shyiko/jabba/releases/download/${JABBA_VERSION}/jabba-${JABBA_VERSION}-windows-amd64.exe
    ;;
  *)
    echo Unsupported OS: $OSTYPE
    exit 1
    ;;
esac

curl -L -o jabba "$BINARY_URL"
chmod +x jabba
