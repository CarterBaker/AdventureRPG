package engine.util.log;

public enum LogLevel {

    /*
     * Severity of a captured log line. An ERROR line is what turns a session
     * log into a file on disk; INFO lines are only kept in memory until then.
     */

    INFO,
    ERROR;
}
