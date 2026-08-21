ARG version
FROM gcr.io/datadoghq/dd-lib-java-init:v${version} AS datadog
FROM busybox:1.37.0
COPY --from=datadog /datadog-init/package/dd-java-agent.jar /javaagent.jar

