#!/usr/bin/env bash
# This file needs to be sourced!

# Default assumed home path. Might get changed during Jabba install.
JABBA_HOME=~/.jabba
JABBA_SH=$JABBA_HOME/jabba.sh

# Skip if Jabba is already installed:
if [ -s $JABBA_SH ]; then
  source $JABBA_SH
  JABBA_VERSION=$(jabba --version)
  echo Jabba is already installed: $JABBA_VERSION
  return
fi

echo Installing Jabba ...
pushd "$(dirname "$BASH_SOURCE")"
source jabbaInstaller.sh --skip-rc
[ -s "$JABBA_HOME/jabba.sh" ] && source "$JABBA_HOME/jabba.sh"
popd
