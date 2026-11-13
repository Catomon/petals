package org.slf4j;

/** Logs nothing, which is what the browser build wants. */
public final class LoggerFactory {

    private static final Logger NOP = new NopLogger();

    private LoggerFactory() {
    }

    public static Logger getLogger(String name) {
        return NOP;
    }

    public static Logger getLogger(Class<?> type) {
        return NOP;
    }

    private static final class NopLogger implements Logger {
        @Override public String getName() { return "nop"; }
        @Override public boolean isTraceEnabled() { return false; }
        @Override public boolean isDebugEnabled() { return false; }
        @Override public boolean isInfoEnabled() { return false; }
        @Override public boolean isWarnEnabled() { return false; }
        @Override public boolean isErrorEnabled() { return false; }
        @Override public void trace(String message) { }
        @Override public void trace(String message, Throwable throwable) { }
        @Override public void trace(String format, Object arg) { }
        @Override public void trace(String format, Object arg1, Object arg2) { }
        @Override public void trace(String format, Object... arguments) { }
        @Override public void debug(String message) { }
        @Override public void debug(String message, Throwable throwable) { }
        @Override public void debug(String format, Object arg) { }
        @Override public void debug(String format, Object arg1, Object arg2) { }
        @Override public void debug(String format, Object... arguments) { }
        @Override public void info(String message) { }
        @Override public void info(String message, Throwable throwable) { }
        @Override public void info(String format, Object arg) { }
        @Override public void info(String format, Object arg1, Object arg2) { }
        @Override public void info(String format, Object... arguments) { }
        @Override public void warn(String message) { }
        @Override public void warn(String message, Throwable throwable) { }
        @Override public void warn(String format, Object arg) { }
        @Override public void warn(String format, Object arg1, Object arg2) { }
        @Override public void warn(String format, Object... arguments) { }
        @Override public void error(String message) { }
        @Override public void error(String message, Throwable throwable) { }
        @Override public void error(String format, Object arg) { }
        @Override public void error(String format, Object arg1, Object arg2) { }
        @Override public void error(String format, Object... arguments) { }
    }
}
