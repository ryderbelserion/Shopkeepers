#!/usr/bin/env bash
if [ $# -eq 0 ]; then
    echo "Missing argument(s)"
    exit 1
fi

mkdir -p $HOME/spigot-build
pushd $HOME/spigot-build
echo "Downloading Spigot BuildTools for Minecraft version $1"
curl https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar -o $HOME/spigot-build/BuildTools.jar

# Save status of core.autocrlf
git_autocrlf=`git config --global core.autocrlf`
# Unset autocrlf, required by BuildTools under linux
git config --global --unset core.autocrlf

# Build spigot
echo "Building Spigot using Spigot BuildTools for Minecraft version $1 (this might take a while)"
java -Xmx1500M -jar BuildTools.jar --rev $1 --compile CRAFTBUKKIT,SPIGOT --remapped | grep Installing

# Reset autocrlf
git config --global core.autocrlf $git_autocrlf
popd
