package com.mycompany.antifraud;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.logging.structured.StructuredLogFormatter;

import java.util.List;
import java.util.stream.Collectors;

public class LogfmtStructuredLogFormatter implements StructuredLogFormatter<ILoggingEvent> {
    @Override
    public String format(ILoggingEvent event) {

        List<KeyValuePair> kvList = event.getKeyValuePairs();
        String kvPairs = (kvList == null || kvList.isEmpty()) ? "" :
                " " + kvList.stream()
                        .map(kv -> kv.key + "=" + kv.value)
                        .collect(Collectors.joining(" "));

        String exception = event.getThrowableProxy() == null ? "" :
                " exception.message=\"" + event.getThrowableProxy().getMessage() + "\" exception.type=" + event.getThrowableProxy().getClassName();

        return event.getInstant()
                + " " + event.getLevel()
                + " " + event.getLoggerName()
                + " " + event.getFormattedMessage()
                + kvPairs
                + exception
                +"\n";

//
//        return event.getInstant()
//                + " level=" + event.getLevel()
//                + " thread=\"" + event.getThreadName() + "\"
//                + " logger=" + event.getLoggerName()
//                + " message=\"" + event.getFormattedMessage() + "\""
//                + kvPairs
//                + exception
//                +"\n";
    }
}
