#!/usr/bin/env bash
# set -x

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

# LOAD ENVIRONMENT VARIABLES
if [ -r "$PRGDIR/../setenv.sh" ]; then
  . "$PRGDIR/../setenv.sh"
else
  . "$PRGDIR/../setenv.default.sh"
fi
if [ -r "$PRGDIR/setenv.sh" ]; then
  . "$PRGDIR/setenv.sh"
fi

export OPEN_TELEMETRY_AGENT_HOME=$PRGDIR/../.otel
mkdir -p "$OPEN_TELEMETRY_AGENT_HOME"

##########################################################################################
# DOWNLOAD OPEN TELEMETRY AGENT IF NOT FOUND
# code copied from Maven Wrappers's mvnw`
##########################################################################################
export OPEN_TELEMETRY_AGENT_JAR=$OPEN_TELEMETRY_AGENT_HOME/opentelemetry-javaagent-$OPEN_TELEMETRY_AGENT_VERSION.jar
if [ -r "$OPEN_TELEMETRY_AGENT_JAR" ]; then
    echo "Found $OPEN_TELEMETRY_AGENT_JAR"
else
    echo "Couldn't find $OPEN_TELEMETRY_AGENT_JAR, downloading it ..."
    jarUrl="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v$OPEN_TELEMETRY_AGENT_VERSION/opentelemetry-javaagent.jar"

    if command -v wget > /dev/null; then
        wget "$jarUrl" -O "$OPEN_TELEMETRY_AGENT_JAR"
    elif command -v curl > /dev/null; then
        curl -o "$OPEN_TELEMETRY_AGENT_JAR" "$jarUrl"
    else
        echo "FAILURE: OpenTelemetry agent not found and  none of curl and wget found"
        exit 1;
    fi
fi

$PRGDIR/../mvnw -DskipTests package

echo "##################"
echo "# START FRONTEND #"
echo "##################"
echo ""
echo "OTEL_EXPORTER_OTLP_ENDPOINT: $OTEL_EXPORTER_OTLP_ENDPOINT"

export OTEL_RESOURCE_ATTRIBUTES="service.name=frontend,service.namespace=com-shoppingcart,service.version=1.0-SNAPSHOT,deployment.environment=$OPEN_TELEMETRY_DEPLOYMENT_ENVIRONMENT"
export OTEL_METRICS_EXPORTER="otlp"
export OTEL_LOGS_EXPORTER="otlp"

java -javaagent:$PRGDIR/../.otel/opentelemetry-javaagent-$OPEN_TELEMETRY_AGENT_VERSION.jar \
     -Dserver.port=8080 \
     -jar target/frontend-1.0-SNAPSHOT.jar
