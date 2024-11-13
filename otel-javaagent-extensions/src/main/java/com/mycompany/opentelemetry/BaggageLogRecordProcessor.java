package com.mycompany.opentelemetry;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.ReadWriteLogRecord;
import io.opentelemetry.sdk.logs.data.LogRecordData;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BaggageLogRecordProcessor implements LogRecordProcessor {

    private final Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public void onEmit(Context context, ReadWriteLogRecord logRecord) {
        Baggage.fromContext(context).forEach((baggageEntryName, baggageEntry) -> {
            logger.log(Level.FINEST, () -> {
                LogRecordData logRecordData = logRecord.toLogRecordData();
                return "logRecord['" +
                        logRecordData.getInstrumentationScopeInfo().getName() + ": " +
                        logRecordData.getBodyValue() +
                        ".attribute[" + baggageEntryName + "]=" + baggageEntry.getValue();
            });
            logRecord.setAttribute(AttributeKey.stringKey(baggageEntryName), baggageEntry.getValue());
        });
    }
}
