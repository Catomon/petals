package org.apache.log4j;

/** Minimal Log4j surface used by libraries on the web classpath. */
public class Logger {

    private final String name;

    private Logger(String name) {
        this.name = name;
    }

    public static Logger getLogger(String name) {
        return new Logger(name);
    }

    public static Logger getLogger(Class<?> type) {
        return new Logger(type.getName());
    }

    public static Logger getRootLogger() {
        return new Logger("root");
    }

    public String getName() {
        return name;
    }

    public boolean isTraceEnabled() { return false; }

    public boolean isDebugEnabled() { return false; }

    public boolean isInfoEnabled() { return false; }

    public void trace(Object message) { }

    public void debug(Object message) { }

    public void debug(Object message, Throwable throwable) { }

    public void info(Object message) { }

    public void info(Object message, Throwable throwable) { }

    public void warn(Object message) { }

    public void warn(Object message, Throwable throwable) { }

    public void error(Object message) { }

    public void error(Object message, Throwable throwable) { }
}
