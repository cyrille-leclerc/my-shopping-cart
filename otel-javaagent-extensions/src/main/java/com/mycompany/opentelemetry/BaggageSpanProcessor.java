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

    @Override
    public void onStart(Context context, ReadWriteSpan span) {

        if (span.getKind() == SpanKind.SERVER || span.getKind() == SpanKind.CONSUMER) {
            Baggage.fromContext(context).forEach((baggageEntryName, baggageEntry) -> {
                logger.log(Level.FINEST, () -> "Span['" +
                        span.getName() + "' / " + span.getKind() + ", " +
                        "spanId=" + span.getSpanContext().getSpanId() + ", " +
                        "traceId=" + span.getSpanContext().getTraceId() + "]" +
                        ".attribute[" + baggageEntryName + "]=" + baggageEntry.getValue());
                span.setAttribute(baggageEntryName, baggageEntry.getValue());
            });
        }
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
