package org.slf4j;

/** Minimal SLF4J surface used by libraries on the web classpath. */
public interface Logger {
    String getName();

    boolean isTraceEnabled();

    boolean isDebugEnabled();

    boolean isInfoEnabled();

    boolean isWarnEnabled();

    boolean isErrorEnabled();

    void trace(String message);

    void trace(String message, Throwable throwable);

    void trace(String format, Object arg);

    void trace(String format, Object arg1, Object arg2);

    void trace(String format, Object... arguments);

    void debug(String message);

    void debug(String message, Throwable throwable);

    void debug(String format, Object arg);

    void debug(String format, Object arg1, Object arg2);

    void debug(String format, Object... arguments);

    void info(String message);

    void info(String message, Throwable throwable);

    void info(String format, Object arg);

    void info(String format, Object arg1, Object arg2);

    void info(String format, Object... arguments);

    void warn(String message);

    void warn(String message, Throwable throwable);

    void warn(String format, Object arg);

    void warn(String format, Object arg1, Object arg2);

    void warn(String format, Object... arguments);

    void error(String message);

    void error(String message, Throwable throwable);

    void error(String format, Object arg);

    void error(String format, Object arg1, Object arg2);

    void error(String format, Object... arguments);
}
