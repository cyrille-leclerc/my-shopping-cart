package com.mycompany.opentelemetry;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BaggageSpanProcessor implements SpanProcessor {
    private final Logger logger = Logger.getLogger(getClass().getName());

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
            logger.log(Level.FINEST, () -> "Span['" +
                    span.getName() + "' / " + span.getKind() + ", " +
                    "spanId=" + span.getSpanContext().getSpanId() + ", " +
                    "traceId=" + span.getSpanContext().getTraceId() + "]" +
                    ".attribute[" + baggageEntryName + "]=" + baggageEntry.getValue());
            span.setAttribute(baggageEntryName, baggageEntry.getValue());
        });
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {

    }

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
