package com.mycompany.warehouse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Random;

@Component
public class OrderProcessor {
    final static Random RANDOM = new Random();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void receiveMessage(byte[] message) throws Exception {
        int millis = RANDOM.nextInt(100);
        logger.info("Process " + new String(message, StandardCharsets.UTF_8) + " sleep " + millis + "ms");
        Thread.sleep(millis);
    }
}
