#!/usr/bin/env bash

##########################################################################################
# PARENT DIRECTORY
# code copied from Tomcat's `catalina.sh`
##########################################################################################
# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

export OPEN_TELEMETRY_AGENT_HOME=$PRGDIR/../.otel
mkdir -p "$OPEN_TELEMETRY_AGENT_HOME"

# otelcontribcol-dc17498 -> https://github.com/open-telemetry/opentelemetry-collector-contrib/commit/dc17498a84a16fac6769983b595f0ab183201a06
export OPENTELEMETRY_COLLECTOR_PATH=otelcontribcol-dc17498

echo "WARNING hardcoded opentelemetry collector executable: $OPENTELEMETRY_COLLECTOR_PATH, to change the executable path, edit $0"

set -x

$OPENTELEMETRY_COLLECTOR_PATH --config $PRGDIR/opentelemetry-collector-exporter-elastic.yaml