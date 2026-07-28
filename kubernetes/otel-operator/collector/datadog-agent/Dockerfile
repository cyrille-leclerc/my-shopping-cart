FROM gcr.io/datadoghq/agent:7

# See https://github.com/open-telemetry/opentelemetry-collector-releases/blob/main/distributions/

COPY run-datadog-agent /run-datadog-agent
RUN chmod +x /run-datadog-agent


ENTRYPOINT ["/run-datadog-agent"]

# datadog trace-agent: 8126
#
EXPOSE 8126