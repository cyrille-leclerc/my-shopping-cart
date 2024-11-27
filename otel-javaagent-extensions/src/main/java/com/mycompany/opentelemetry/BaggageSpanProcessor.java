package com.mycompany.opentelemetry;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Set {@link Baggage} entries as attributes on the {@link io.opentelemetry.api.trace.Span}.
 * <p>
 * Similar to <a href="https://github.com/open-telemetry/opentelemetry-java-contrib/blob/v1.41.0/baggage-processor/src/main/java/io/opentelemetry/contrib/baggage/processor/BaggageSpanProcessor.java">BaggageSpanProcessor</a>.
 */
public class BaggageSpanProcessor implements SpanProcessor {

    private final Logger logger = Logger.getLogger(getClass().getName());

    final Predicate<String> baggageEntryNameFilter;

    public BaggageSpanProcessor() {
        // TODO in production, implement mechanism to filter baggage entries
        baggageEntryNameFilter = Predicates.alwaysTrue();
        logger.log(Level.FINE, () -> "BaggageSpanProcessor initialized with baggageEntryNameFilter: " + baggageEntryNameFilter);
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    /**
     * Set {@link Baggage} entries as attributes on the Span just after {@link io.opentelemetry.api.trace.Span} creation
     * to allow instrumentation libs and custom code to set attribute values that would override baggage values.
     *
     * @param context the parent {@code Context} of the span that just started.
     * @param span    the {@code Span} that just started.
     */
    @Override
    public void onStart(Context context, ReadWriteSpan span) {
        Baggage.fromContext(context).forEach((baggageEntryName, baggageEntry) -> {
            if (baggageEntryNameFilter.test(baggageEntryName)) {
                span.setAttribute(baggageEntryName, baggageEntry.getValue());
            }
        });
    }

    @Override
    public boolean isEndRequired() {
        return false;
    }

    @Override
    public void onEnd(ReadableSpan span) {

    }
}
