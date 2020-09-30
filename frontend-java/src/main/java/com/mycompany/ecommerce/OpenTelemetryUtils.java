package com.mycompany.ecommerce;

import io.opentelemetry.trace.Span;

import java.io.PrintWriter;
import java.io.StringWriter;

public class OpenTelemetryUtils {

    public static void recordException(Span span, Throwable e) {
        // see https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/813
        // see https://github.com/open-telemetry/opentelemetry-specification/blob/master/specification/trace/semantic_conventions/exceptions.md
        span.setAttribute("exception.message\t", e.getMessage());
        span.setAttribute("exception.type", e.getClass().getName());

        StringWriter errorString = new StringWriter();
        e.printStackTrace(new PrintWriter(errorString));
        span.setAttribute("exception.stacktrace", errorString.toString());
    }
}
