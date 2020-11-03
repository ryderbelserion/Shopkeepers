#!/usr/bin/env bash

# We only re-build CraftBukkit/Spigot versions that are missing in the Maven cache.
# Add entries here for every required version of CraftBukkit/Spigot:
if [ ! -f "$HOME/.m2/repository/org/bukkit/craftbukkit/1.14.4-R0.1-SNAPSHOT/craftbukkit-1.14.4-R0.1-SNAPSHOT.jar" ]; then ./installSpigot.sh 1.14.4 ; else echo "Not compiling Spigot 1.14.4 because it is already in our Maven repo" ; fi
if [ ! -f "$HOME/.m2/repository/org/bukkit/craftbukkit/1.15.2-R0.1-SNAPSHOT/craftbukkit-1.15.2-R0.1-SNAPSHOT.jar" ]; then ./installSpigot.sh 1.15.2 ; else echo "Not compiling Spigot 1.15.2 because it is already in our Maven repo" ; fi
if [ ! -f "$HOME/.m2/repository/org/bukkit/craftbukkit/1.16.1-R0.1-SNAPSHOT/craftbukkit-1.16.1-R0.1-SNAPSHOT.jar" ]; then ./installSpigot.sh 1.16.1 ; else echo "Not compiling Spigot 1.16.1 because it is already in our Maven repo" ; fi
if [ ! -f "$HOME/.m2/repository/org/bukkit/craftbukkit/1.16.2-R0.1-SNAPSHOT/craftbukkit-1.16.2-R0.1-SNAPSHOT.jar" ]; then ./installSpigot.sh 1.16.2 ; else echo "Not compiling Spigot 1.16.2 because it is already in our Maven repo" ; fi
if [ ! -f "$HOME/.m2/repository/org/bukkit/craftbukkit/1.16.4-R0.1-SNAPSHOT/craftbukkit-1.16.4-R0.1-SNAPSHOT.jar" ]; then ./installSpigot.sh 1.16.4 ; else echo "Not compiling Spigot 1.16.4 because it is already in our Maven repo" ; fi
