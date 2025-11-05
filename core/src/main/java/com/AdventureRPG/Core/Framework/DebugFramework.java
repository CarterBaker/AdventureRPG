package com.AdventureRPG.Core.Framework;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public abstract class DebugFramework {

    // Debug \\

    private final String systemName = getClass().getSimpleName();

    protected final void debug() {
        debug("");
    }

    protected final void debug(String input) {
        System.out.println("[" + systemName + "] " + input);
    }

    protected final void log(String input) {
        System.out.println(input);
    }

    protected final void error(String input) {
        System.err.println(input);
    }

    protected final void timeStamp(String input) {
        String time = LocalTime.now().format(TIME_FORMAT);
        System.out.println("[" + time + "] " + input);
    }

    // Utility \\

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
}
