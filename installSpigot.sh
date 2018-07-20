#!/usr/bin/env bash
mkdir -p $HOME/spigot-build
pushd $HOME/spigot-build
echo "Downloading Spigot Build Tools for minecraft version $1"
curl https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar -o $HOME/spigot-build/BuildTools.jar
git config --global --unset core.autocrlf
echo "Building Spigot using Spigot Build Tools for minecraft version $1 (this might take a while)"
java -Xmx1500M -jar BuildTools.jar --rev $1 | grep Installing
popd
