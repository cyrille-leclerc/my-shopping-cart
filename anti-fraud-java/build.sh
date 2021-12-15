#!/usr/bin/env bash

if [ -r "setenv.sh" ]; then
  . "setenv.sh"
fi

mvn -X -Dmaven.ext.class.path=/Users/cyrilleleclerc/git/opentelemetry/opentelemetry-java-contrib/maven-extension/build/libs/opentelemetry-maven-extension-1.10.0-SNAPSHOT.jar \
    snyk:test snyk:monitor spring-boot:build-image deploy
