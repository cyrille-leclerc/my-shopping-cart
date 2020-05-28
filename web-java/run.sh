#!/usr/bin/env bash
set -x

export OPEN_TELEMETRY_AGENT_HOME=/opt/opentelemetry-java-agent
export OPEN_TELEMETRY_AGENT_VERSION=0.3.0
export OPEN_TELEMETRY_AGENT_JAR=$OPEN_TELEMETRY_AGENT_HOME/opentelemetry-auto-$OPEN_TELEMETRY_AGENT_VERSION.jar
# export OPEN_TELEMETRY_AGENT_EXPORTER_JAR=$OPEN_TELEMETRY_AGENT_HOME/opentelemetry-auto-exporters-logging-$OPEN_TELEMETRY_AGENT_VERSION.jar

# JAEGER
export OPEN_TELEMETRY_AGENT_EXPORTER_JAR=$OPEN_TELEMETRY_AGENT_HOME/opentelemetry-auto-exporters-jaeger-$OPEN_TELEMETRY_AGENT_VERSION.jar
export OTEL_RESOURCE_ATTRIBUTES=service.name=web_jaeger,service.namespace=com-shoppingcart,service.version=1.0-SNAPSHOT

# OTEL COLLECTOR
# export OPEN_TELEMETRY_AGENT_EXPORTER_JAR=$OPEN_TELEMETRY_AGENT_HOME/opentelemetry-auto-exporters-otlp-$OPEN_TELEMETRY_AGENT_VERSION.jar
# export OTEL_RESOURCE_ATTRIBUTES=service.name=web_otlp,service.namespace=com-shoppingcart,service.version=1.0-SNAPSHOT

java -javaagent:$OPEN_TELEMETRY_AGENT_JAR \
     -Dota.exporter.jar=$OPEN_TELEMETRY_AGENT_EXPORTER_JAR \
     -Dota.exporter.otlp.endpoint=localhost:55680 \
     -Dota.exporter.jaeger.endpoint=localhost:14250 \
     -Dota.exporter.jaeger.service.name=web_jaeger \
     -Dserver.port=8080 \
     -Dio.opentelemetry.auto.slf4j.simpleLogger.defaultLogLevel=info \
     -jar target/web-1.0-SNAPSHOT.jar