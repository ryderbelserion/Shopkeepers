#!/usr/bin/env bash

# We require Java 16 to build:
source ./installJDK.sh 16

# Build via Maven:
./mvnw clean install
