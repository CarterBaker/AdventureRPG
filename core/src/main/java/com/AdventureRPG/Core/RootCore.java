package com.AdventureRPG.Core;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.AdventureRPG.SettingsSystem.Settings;

abstract class RootCore {

    void internalInit(
            Settings settings,
            RootManager rootManager) {
    }

    void internalAwake() {
    }

    void internalStart() {
    }

    void internalMenuExclusiveUpdate() {
    }

    void internalGameExclusiveUpdate() {
    }

    void internalUpdate() {
    }

    void internalFixedUpdate() {
    }

    void internalLateUpdate() {
    }

    void internalRender() {
    }

    void internalDispose() {
    }

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
