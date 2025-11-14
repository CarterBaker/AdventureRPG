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

    protected final void debug(String input) {
        System.out.println("[" + systemName + "] " + input);
    }

    protected final void timeStampDebug(String input) {
        debug(timeStamp() + input);
    }

    // Log \\

    protected final void log(String input) {
        System.out.println(input);
    }

    protected final void error(String input) {
        System.err.println(input);
    }

    protected final void timeStampLog(String input) {
        System.out.println(timeStamp() + input);
    }

    // Utility \\

    protected final String timeStamp() {
        String time = LocalTime.now().format(TIME_FORMAT);
        return "[" + time + "] ";
    }

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
}
