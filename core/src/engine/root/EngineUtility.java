package engine.root;

public abstract class EngineUtility {

    /*
     * EngineUtility provides engine-level diagnostics and exception
     * handling to static utility classes.
     *
     * Since static helpers cannot extend UtilityPackage directly,
     * this class exposes a shared internal UtilityPackage instance
     * and forwards its functionality through static methods.
     *
     * This allows static systems to participate in the same
     * debugging and failure-reporting infrastructure as the rest
     * of the engine.
     */

    // Internal \\

    protected static final UtilityPackage UTILITY = new UtilityPackage() {
    };

    // Debug \\

    public static void debug() {
        UTILITY.debug();
    }

    public static void debug(Object input) {
        UTILITY.debug(input);
    }

    public static void timeStampDebug(Object input) {
        UTILITY.timeStampDebug(input);
    }

    // Log \\

    public static void log(Object input) {
        UTILITY.log(input);
    }

    public static void errorLog(Object input) {
        UTILITY.errorLog(input);
    }

    public static void timeStampLog(Object input) {
        UTILITY.timeStampLog(input);
    }

    // Exception Handling \\

    public static <T> T throwException() {
        return UTILITY.throwException();
    }

    public static <T> T throwException(String message) {
        return UTILITY.throwException(message);
    }

    public static <T> T throwException(Throwable cause) {
        return UTILITY.throwException(cause);
    }

    public static <T> T throwException(String message, Throwable cause) {
        return UTILITY.throwException(message, cause);
    }

    public static <T> T throwException(Object input) {
        return UTILITY.throwException(input);
    }

    public static <T> T throwException(Object input, Throwable cause) {
        return UTILITY.throwException(input, cause);
    }
}
