package engine.util.log;

import engine.root.StructPackage;

public class LogLineStruct extends StructPackage {

    /*
     * One captured line of the session log: its severity, the time it was
     * written, and its text. Immutable once written.
     */

    // Line
    private final LogLevel logLevel;
    private final String timeStamp;
    private final String text;

    // Internal \\

    public LogLineStruct(
            LogLevel logLevel,
            String timeStamp,
            String text) {

        // Line
        this.logLevel = logLevel;
        this.timeStamp = timeStamp;
        this.text = text;
    }

    // Accessible \\

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getText() {
        return text;
    }
}
