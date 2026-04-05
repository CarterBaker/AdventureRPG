package program.core.engine;

public abstract class UtilityPackage {

    /*
     * UtilityPackage provides engine-level diagnostics and exception
     * handling to static utility classes.
     *
     * Since static helpers cannot extend EngineUtility directly,
     * this class exposes a shared internal EngineUtility instance
     * and forwards its functionality through static methods.
     *
     * This allows static systems to participate in the same
     * debugging and failure-reporting infrastructure as the rest
     * of the engine.
     */

    // Internal \\

    protected static final EngineUtility ENGINE = new EngineUtility() {
    };

    // Debug \\

    public static void debug() {
        ENGINE.debug();
    }

    public static void debug(Object input) {
        ENGINE.debug(input);
    }

    public static void timeStampDebug(Object input) {
        ENGINE.timeStampDebug(input);
    }

    // Log \\

    public static void log(Object input) {
        ENGINE.log(input);
    }

    public static void errorLog(Object input) {
        ENGINE.errorLog(input);
    }

    public static void timeStampLog(Object input) {
        ENGINE.timeStampLog(input);
    }

    // Exception Handling \\

    public static <T> T throwException() {
        return ENGINE.throwException();
    }

    public static <T> T throwException(String message) {
        return ENGINE.throwException(message);
    }

    public static <T> T throwException(Throwable cause) {
        return ENGINE.throwException(cause);
    }

    public static <T> T throwException(String message, Throwable cause) {
        return ENGINE.throwException(message, cause);
    }

    public static <T> T throwException(Object input) {
        return ENGINE.throwException(input);
    }

    public static <T> T throwException(Object input, Throwable cause) {
        return ENGINE.throwException(input, cause);
    }
}
