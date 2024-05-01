#!/usr/bin/env bash

pushd "$(dirname "$BASH_SOURCE")"

# $1: The directory path to delete
deleteDirectory() {
  if [ -d "$1" ]; then
    echo "Deleting $1"
    rm -r $1
  else
    echo "Skipping non-existent path $1"
  fi
}

echo "Deleting local Spigot dependencies."

deleteDirectory $HOME/spigot-build
deleteDirectory $HOME/.m2/repository/org/bukkit/craftbukkit
deleteDirectory $HOME/.m2/repository/org/bukkit/bukkit
deleteDirectory $HOME/.m2/repository/org/spigotmc/spigot
deleteDirectory $HOME/.m2/repository/org/spigotmc/spigot-api
deleteDirectory $HOME/.m2/repository/org/spigotmc/minecraft-server
deleteDirectory $HOME/.m2/repository/org/spigotmc/spigot-parent

popd
