/*
 * Copyright 2024 Giuliano Gorgone
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.giulianogorgone.fluidswipe.utils.log;

import java.lang.annotation.Native;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author Giuliano Gorgone (anticleiades)
 */
public final class Logging {
    private Logging() {
    }

    // Object used to mark logging requests originating from the native side.
    private static final Object NATIVE_LOG_MARKER = new Object();
    //=======================================================//
    // Constants corresponding, on the native side, to each java.util.logging.Level
    //=======================================================//
    @Native
    private static final int OFF = 0;
    @Native
    private static final int SEVERE = 1;
    @Native
    private static final int WARNING = 2;
    @Native
    private static final int INFO = 3;
    @Native
    private static final int CONFIG = 4;
    @Native
    private static final int FINE = 5;
    @Native
    private static final int FINER = 6;
    @Native
    private static final int FINEST = 7;
    @Native
    private static final int ALL = 8;
    // Used as an immutable dictionary to efficiently gather the corresponding level using the appropriate constant.
    private static final Level[] intLvlDict = {
            Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST, Level.ALL
    };
    private static Logger LOG; // our logger
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final Pattern FILE_EXT_PATTERN = Pattern.compile("\\.");

    public static void init(final String loggerName, final Level logLevel, final String logFilePath) {
        LOG = Logger.getLogger(loggerName);
        LOG.setUseParentHandlers(false);
        final ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new DefaultLogFormatter());
        LOG.addHandler(consoleHandler);
        if (logFilePath != null) {
            try {
                final FileHandler fileHandler = new FileHandler(logFilePath);
                fileHandler.setFormatter(new DefaultLogFormatter());
                fileHandler.setLevel(logLevel);
                LOG.addHandler(fileHandler);
            } catch (Exception e) {
                logSevere("an error occurred while enabling file logging; path: " + logFilePath);
            }
        }
        consoleHandler.setLevel(logLevel);
        LOG.setLevel(logLevel);
    }

    private static int lvlToInt(Level lvl) {
        for (int i = 0; i < intLvlDict.length; i++)
            if (lvl == intLvlDict[i]) return i;
        return OFF;
    }

    // Enables the native side to perform logging request using the java.logging facility.
    // Any platform-specific implementation library must expose the "_Java_eu_giulianogorgone_fluidswipe_utils_log_Logging_initNative" symbol.
    private static native void initNative(final int lvl);

    public static void initNative(Level lvl) {
        initNative(lvlToInt(lvl));
    }

    // called from the native side.
    private static void log(final int lvl, String filename, final String callerMethod, final String msg, final Throwable throwable) {
        if (lvl < OFF || lvl > ALL) throw new IllegalArgumentException();
        filename = FILE_EXT_PATTERN.split(filename)[0]; // remove the file extension
        log(intLvlDict[lvl], filename, callerMethod, msg, throwable, new Object[]{getCallerThreadInfo(), NATIVE_LOG_MARKER});
    }

    static boolean isLogRequestFromNative(final LogRecord record) {
        final Object[] params;
        return (params = record.getParameters()) != null && params.length >= 2 && params[1] == NATIVE_LOG_MARKER;
    }

    //======================//
    // Convenience methods
    //======================//
    public static void logSevere(final String msg) {
        logSevere(msg, null);
    }

    public static void logSevere(final String msg, final Throwable throwable) {
        log(Level.SEVERE, null, null, msg, throwable, null);
    }

    public static void logFinest(final String msg) {
        log(Level.FINEST, null, null, msg, null, null);
    }

    public static void logConfig(final String msg) {
        log(Level.CONFIG, null, null, msg, null, null);
    }

    public static void logWarn(final String msg) {
        log(Level.WARNING, null, null, msg, null, null);
    }

    // Used to add caller thread information.
    static void log(final Level level, String sourceClassName, String sourceMethodName, final String msg, final Throwable throwable, final Object[] params) {
        final LogRecord record = new LogRecord(level, msg);
        if (sourceClassName == null || sourceMethodName == null) { // getting the caller
            final Optional<StackWalker.StackFrame> frameOpt = WALKER.walk((s) -> s.dropWhile((frame) -> frame.getDeclaringClass().equals(Logging.class)).findFirst());
            if (frameOpt.isPresent()) {
                final StackWalker.StackFrame frame = frameOpt.get();
                if (sourceClassName == null) {
                    sourceClassName = frame.getDeclaringClass().getName();
                }
                if (sourceMethodName == null) {
                    sourceMethodName = frame.getMethodName();
                }
            }
        }
        record.setSourceClassName(sourceClassName);
        record.setSourceMethodName(sourceMethodName);
        record.setThrown(throwable);
        record.setParameters(params == null ? new Object[]{getCallerThreadInfo()} : params);
        LOG.log(record);
    }

    private static String getCallerThreadInfo() {
        return '[' + Thread.currentThread().getName() + ']';
    }
}