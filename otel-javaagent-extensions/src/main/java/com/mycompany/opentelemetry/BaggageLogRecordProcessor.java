package com.mycompany.opentelemetry;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;

import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Set {@link Baggage} entries as attributes on the LogRecord
 * <p>
 * Similar to <a href="https://github.com/open-telemetry/opentelemetry-java-contrib/blob/v1.41.0/baggage-processor/src/main/java/io/opentelemetry/contrib/baggage/processor/BaggageSpanProcessor.java">BaggageSpanProcessor</a>.
 */
public class BaggageLogRecordProcessor implements LogRecordProcessor {

    private final Logger logger = Logger.getLogger(getClass().getName());

    final Predicate<String> baggageEntryNameFilter;

    public BaggageLogRecordProcessor() {
        // TODO in production, implement mechanism to filter baggage entries
        baggageEntryNameFilter = Predicates.alwaysTrue();
        logger.log(Level.FINE, () -> "BaggageLogRecordProcessor initialized with baggageEntryNameFilter: " + baggageEntryNameFilter);
    }

    @Override
    public void onEmit(Context context, ReadWriteLogRecord logRecord) {
        Baggage.fromContext(context).forEach((baggageEntryName, baggageEntry) -> {
            if (baggageEntryNameFilter.test(baggageEntryName)) {
                logRecord.setAttribute(AttributeKey.stringKey(baggageEntryName), baggageEntry.getValue());
            }
        });
    }
}
