#!/usr/bin/env bash

# We only re-build CraftBukkit/Spigot versions that are missing in the Maven cache.
# Add entries here for every required version of CraftBukkit/Spigot.

# The following versions require JDK 8 to build:
source ./installJDK.sh 8

if [ ! -f "$HOME/.m2/repository/org/bukkit/craftbukkit/1.14.4-R0.1-SNAPSHOT/craftbukkit-1.14.4-R0.1-SNAPSHOT.jar" ]; then ./installSpigot.sh 1.14.4 ; else echo "Not compiling Spigot 1.14.4 because it is already in our Maven repo" ; fi
if [ ! -f "$HOME/.m2/repository/org/bukkit/craftbukkit/1.15.2-R0.1-SNAPSHOT/craftbukkit-1.15.2-R0.1-SNAPSHOT.jar" ]; then ./installSpigot.sh 1.15.2 ; else echo "Not compiling Spigot 1.15.2 because it is already in our Maven repo" ; fi
if [ ! -f "$HOME/.m2/repository/org/bukkit/craftbukkit/1.16.1-R0.1-SNAPSHOT/craftbukkit-1.16.1-R0.1-SNAPSHOT.jar" ]; then ./installSpigot.sh 1.16.1 ; else echo "Not compiling Spigot 1.16.1 because it is already in our Maven repo" ; fi
if [ ! -f "$HOME/.m2/repository/org/bukkit/craftbukkit/1.16.3-R0.1-SNAPSHOT/craftbukkit-1.16.3-R0.1-SNAPSHOT.jar" ]; then ./installSpigot.sh 1.16.3 ; else echo "Not compiling Spigot 1.16.3 because it is already in our Maven repo" ; fi
if [ ! -f "$HOME/.m2/repository/org/bukkit/craftbukkit/1.16.5-R0.1-SNAPSHOT/craftbukkit-1.16.5-R0.1-SNAPSHOT.jar" ]; then ./installSpigot.sh 1.16.5 ; else echo "Not compiling Spigot 1.16.5 because it is already in our Maven repo" ; fi

# The following versions require JDK 16 to build:
source ./installJDK.sh 16

if [ ! -f "$HOME/.m2/repository/org/bukkit/craftbukkit/1.17-R0.1-SNAPSHOT/craftbukkit-1.17-R0.1-SNAPSHOT-remapped-mojang.jar" ]; then ./installSpigot.sh 1.17 ; else echo "Not compiling Spigot 1.17 because it is already in our Maven repo" ; fi
if [ ! -f "$HOME/.m2/repository/org/bukkit/craftbukkit/1.17.1-R0.1-SNAPSHOT/craftbukkit-1.17.1-R0.1-SNAPSHOT-remapped-mojang.jar" ]; then ./installSpigot.sh 1.17.1 ; else echo "Not compiling Spigot 1.17.1 because it is already in our Maven repo" ; fi
