package com.mycompany.ecommerce;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public final class StressTestUtils {

    private static final int DASH_PER_LINE = 30;

    private static final StressTestUtils instance = new StressTestUtils();

    private static final String SYMBOL_FAILURE = "x";

    private static final String SYMBOL_CONNECTION_FAILURE = "X";

    private static final String SYMBOL_SUCCESS = "-";

    private final static Random RANDOM = new Random();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Outputs a dash in SystemOut and line breaks when necessary
     */
    public static void incrementProgressBarFailure() {
        instance.incrementProgressBar(SYMBOL_FAILURE);
    }

    /**
     * Outputs an 'X' for a connection failure
     */
    public static void incrementProgressBarConnectionFailure() {
        instance.incrementProgressBar(SYMBOL_CONNECTION_FAILURE);
    }

    public static boolean isEndOfLine() {
        return instance._isEndOfLine();
    }
    /**
     * Outputs a dash in SystemOut and line breaks when necessary
     */
    public static void incrementProgressBarSuccess() {
        instance.incrementProgressBar(SYMBOL_SUCCESS);
    }

    public static void writeProgressBarLegend() {
        System.out.println();
        System.out.println(SYMBOL_SUCCESS + " : Success");
        System.out.println(SYMBOL_FAILURE + " : Failure");
    }

    private long lastSampleTime;

    private int progressBarCounter;

    private StressTestUtils() {
        super();
    }

    public static long getLastSampleTime() {
        return instance.lastSampleTime;
    }

    public static int getProgressBarCounter() {
        return instance.progressBarCounter;
    }

    /**
     * @param offsetInMillis
     * @param varianceInMillis
     * @return duration of the pause
     */
    public static int sleep(int offsetInMillis, int varianceInMillis) {
        int sleepDuration = offsetInMillis - (varianceInMillis / 2) + RANDOM.nextInt(varianceInMillis);
        try {
            Thread.sleep(sleepDuration);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted", e);
        }
        return sleepDuration;
    }

    public boolean _isEndOfLine() {
        return this.progressBarCounter % DASH_PER_LINE == 0;
    }
    /**
     * <p>
     * Outputs a dash in SystemOut and line breaks when necessary
     * </p>
     */
    protected synchronized void incrementProgressBar(String symbol) {
        if (this.lastSampleTime == 0) {
            this.lastSampleTime = System.currentTimeMillis();
        }
        System.out.print(symbol);
        this.progressBarCounter++;

        if (_isEndOfLine()) {
            System.out.println();
            long now = System.currentTimeMillis();
            long elapsedDuration = now - this.lastSampleTime;
            String throughput;
            if (elapsedDuration == 0) {
                throughput = "infinite";
            } else {
                long throughputAsInt = (DASH_PER_LINE * 1000) / elapsedDuration;
                throughput = String.valueOf(throughputAsInt);
            }
            System.out.print("[" + dateTimeFormatter.format(LocalDateTime.now()) + " " + StressTestUtils.padStart(throughput, 4, ' ') + " req/s]\t");
            this.lastSampleTime = System.currentTimeMillis();
        }
    }


    /**
     * Returns a string, of length at least {@code minLength}, consisting of {@code string} prepended
     * with as many copies of {@code padChar} as are necessary to reach that length. For example,
     *
     * <ul>
     *   <li>{@code padStart("7", 3, '0')} returns {@code "007"}
     *   <li>{@code padStart("2010", 3, '0')} returns {@code "2010"}
     * </ul>
     *
     * <p>See {@link java.util.Formatter} for a richer set of formatting capabilities.
     *
     * @param string the string which should appear at the end of the result
     * @param minLength the minimum length the resulting string must have. Can be zero or negative, in
     *     which case the input string is always returned.
     * @param padChar the character to insert at the beginning of the result until the minimum length
     *     is reached
     * @return the padded string
     */
    public static String padStart(String string, int minLength, char padChar) {
        if (string.length() >= minLength) {
            return string;
        }
        StringBuilder sb = new StringBuilder(minLength);
        for (int i = string.length(); i < minLength; i++) {
            sb.append(padChar);
        }
        sb.append(string);
        return sb.toString();
    }
}
