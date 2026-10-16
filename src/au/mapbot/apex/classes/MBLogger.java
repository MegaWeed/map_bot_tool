/*
 * MBLogger
 * v1.0
 * 2025-01-20
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.classes;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.*;

public class MBLogger {

    static Logger logger;
    static FileHandler fh;
    static FileHandler fhDebug;

    static {
        try {
            fh = new FileHandler("./app.log", false);
            fh.setFormatter(new SimpleMessageOnlyFormatter());

            fhDebug = new FileHandler("./debug.log", false);
            fhDebug.setFormatter(new SimpleFormatter());
            fhDebug.setLevel(Level.WARNING); // Only WARNING and above
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        init();
        setupGlobalWarningLogger();
    }

    public static void log(String logText) {
        if (logger == null) {
            init();
        }
        logger.info(logText);
    }

    public static void warning(String logText, Exception ex) {
        if (logger == null) {
            init();
        }
        if (ex != null) {
            String stacktraceString = Arrays.toString(ex.getStackTrace());
            logger.log(Level.WARNING, logText + " | message: " + ex.getMessage() + " | trace: " + stacktraceString);
        } else {
            logger.log(Level.WARNING, logText);
        }
    }

    private static void setupGlobalWarningLogger() {
        Logger rootLogger = Logger.getLogger("");
        fhDebug.setFilter(record -> record.getLevel().intValue() >= Level.WARNING.intValue());
        rootLogger.addHandler(fhDebug);
    }

//    public static void disableDebug() {
//        Logger rootLogger = Logger.getLogger("");
//        rootLogger.removeHandler(fhDebug);
//    }

    public static void init() {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");

        logger = Logger.getLogger("logger");
        logger.setUseParentHandlers(false);

        fh.setLevel(Level.ALL);
        logger.addHandler(fh);

        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);
        ch.setFormatter(new SimpleFormatter());
        logger.addHandler(ch);

        logger.setLevel(Level.ALL);
    }
    public static class SimpleMessageOnlyFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            sb.append(record.getMessage()).append(System.lineSeparator());
            if (record.getThrown() != null) {
                for (StackTraceElement ste : record.getThrown().getStackTrace()) {
                    sb.append("\tat ").append(ste.toString()).append(System.lineSeparator());
                }
            }
            return sb.toString();
        }
    }
}
