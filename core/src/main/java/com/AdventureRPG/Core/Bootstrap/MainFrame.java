package com.AdventureRPG.Core.Bootstrap;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

abstract class MainFrame {

    // Core \\

    private final String systemName = getClass().getSimpleName();

    // Debug \\

    protected final void debug() {
        debug("");
    }

    protected final void debug(Object input) {
        System.out.println("[" + systemName + "] " + String.valueOf(input));
    }

    protected final void timeStampDebug(Object input) {
        debug(timeStamp() + String.valueOf(input));
    }

    // Log \\

    protected final void log(Object input) {
        System.out.println(String.valueOf(input));
    }

    protected final void error(Object input) {
        System.err.println(String.valueOf(input));
    }

    protected final void timeStampLog(Object input) {
        System.out.println(timeStamp() + String.valueOf(input));
    }

    // Utility \\

    protected final String timeStamp() {
        String time = LocalTime.now().format(TIME_FORMAT);
        return "[" + time + "] ";
    }

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
}
