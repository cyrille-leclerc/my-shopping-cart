ARG version
FROM gcr.io/datadoghq/dd-lib-python-init:v${version} AS datadog
FROM busybox:1.37.0
# Datadog's bootstrap selects the wheel matching the application's Python ABI,
# architecture, and libc at application startup.
COPY --from=datadog /datadog-init/package/ /autoinstrumentation/
