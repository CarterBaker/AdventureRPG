package engine.util.log;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LogUtility extends EngineUtility {

    /*
     * Owns the session log. Every engine log call and every line written to
     * standard out or standard error lands here instead of a console, kept in
     * a bounded history the editor console reads from. The first error opens
     * a file in the log directory and writes the history into it, and every
     * line after that follows, so only a session that errored or crashed
     * leaves a log file behind.
     */

    // Format
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter
            .ofPattern(EngineSetting.LOG_TIME_PATTERN);
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter
            .ofPattern(EngineSetting.LOG_FILE_TIMESTAMP_PATTERN);
    private static final Pattern LINE_BREAK = Pattern.compile(EngineSetting.LOG_LINE_BREAK_PATTERN);

    // Lock
    private static final Object LOCK = new Object();

    // History
    private static final LogLineStruct[] history = new LogLineStruct[EngineSetting.LOG_HISTORY_CAPACITY];
    private static long lineCount;

    // File
    private static File logFile;
    private static PrintWriter logWriter;
    private static boolean logFileFailed;

    // Session \\

    public static void openSession(File baseGameDirectory, String sessionName) {

        synchronized (LOCK) {
            logFile = new File(
                    resolveLogDirectory(baseGameDirectory),
                    String.format(
                            EngineSetting.LOG_FILE_NAME_FORMAT,
                            sessionName,
                            LocalDateTime.now().format(FILE_TIME_FORMAT)));
        }

        System.setOut(createCaptureStream(LogLevel.INFO));
        System.setErr(createCaptureStream(LogLevel.ERROR));
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, failure) -> throwException("Uncaught failure on thread '" + thread.getName() + "'", failure));
    }

    public static void closeSession() {

        synchronized (LOCK) {

            if (logWriter == null)
                return;

            logWriter.close();
            logWriter = null;
        }
    }

    private static File resolveLogDirectory(File baseGameDirectory) {

        String logDirectory = System.getProperty(EngineSetting.LOG_DIRECTORY_PROPERTY);

        return logDirectory != null
                ? new File(logDirectory)
                : new File(baseGameDirectory, EngineSetting.LOG_DIRECTORY);
    }

    private static PrintStream createCaptureStream(LogLevel logLevel) {
        return new PrintStream(new CaptureStream(logLevel), true, StandardCharsets.UTF_8);
    }

    // Write \\

    public static void info(String text) {
        writeLines(LogLevel.INFO, text);
    }

    public static void error(String text) {
        writeLines(LogLevel.ERROR, text);
    }

    private static void writeLines(LogLevel logLevel, String text) {

        String timeStamp = LocalTime.now().format(TIME_FORMAT);

        synchronized (LOCK) {

            String[] lines = LINE_BREAK.split(text);

            for (int i = 0; i < lines.length; i++)
                appendLine(new LogLineStruct(logLevel, timeStamp, lines[i]));

            if (logLevel == LogLevel.ERROR)
                openLogFile(timeStamp);

            if (logWriter != null)
                logWriter.flush();
        }
    }

    private static void appendLine(LogLineStruct line) {

        history[(int) (lineCount % history.length)] = line;
        lineCount++;

        if (logWriter != null)
            logWriter.println(formatLine(line));
    }

    // File \\

    private static void openLogFile(String timeStamp) {

        if (logWriter != null || logFile == null || logFileFailed)
            return;

        try {
            logFile.getParentFile().mkdirs();
            logWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFile, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            logFileFailed = true;
            appendLine(new LogLineStruct(
                    LogLevel.ERROR,
                    timeStamp,
                    "Log file could not be opened: " + logFile.getAbsolutePath() + " (" + e.getMessage() + ")"));
            return;
        }

        for (long line = getOldestLine(); line < lineCount; line++)
            logWriter.println(formatLine(history[(int) (line % history.length)]));
    }

    // Read \\

    public static long copyLines(long fromLine, ObjectArrayList<LogLineStruct> output) {

        synchronized (LOCK) {

            output.clear();

            for (long line = Math.max(fromLine, getOldestLine()); line < lineCount; line++)
                output.add(history[(int) (line % history.length)]);

            return lineCount;
        }
    }

    private static long getOldestLine() {
        return Math.max(0L, lineCount - history.length);
    }

    // Utility \\

    public static String formatLine(LogLineStruct line) {
        return String.format(EngineSetting.LOG_LINE_FORMAT, line.getTimeStamp(), line.getLogLevel(), line.getText());
    }

    public static String describe(Throwable failure) {

        StringWriter trace = new StringWriter();
        failure.printStackTrace(new PrintWriter(trace));

        return trace.toString();
    }

    private static final class CaptureStream extends OutputStream {

        /*
         * Standard stream replacement. Collects bytes until a line break and
         * hands each finished line to the session log at this stream's level.
         */

        // Line
        private final LogLevel logLevel;
        private final ByteArrayOutputStream pendingLine;

        // Internal \\

        CaptureStream(LogLevel logLevel) {

            // Line
            this.logLevel = logLevel;
            this.pendingLine = new ByteArrayOutputStream();
        }

        // Write \\

        @Override
        public synchronized void write(int value) {

            if (value == '\n') {
                writeLines(logLevel, pendingLine.toString(StandardCharsets.UTF_8));
                pendingLine.reset();
                return;
            }

            if (value != '\r')
                pendingLine.write(value);
        }
    }
}
