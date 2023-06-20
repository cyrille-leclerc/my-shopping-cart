#!/usr/bin/env bash

if [ -r "setenv.sh" ]; then
  . "setenv.sh"
fi

mvn jib:build deploy

# com.google.cloud.tools:jib-maven-plugin:build