'use strict'

// Loaded by the OTel Operator through NODE_OPTIONS before application modules.
const tracer = require('/otel-auto-instrumentation-nodejs/node_modules/dd-trace').init()

// Register Datadog as the provider for application code using @opentelemetry/api.
if (tracer.TracerProvider) {
  new tracer.TracerProvider().register()
}
