package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * SLF4J looks for this class by name when a binding is on the classpath. Pointing it at the
 * no-op factory keeps the browser console clean.
 */
public final class StaticLoggerBinder {

    public static final String REQUESTED_API_VERSION = "1.7.16";

    private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

    private StaticLoggerBinder() {
    }

    public static StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    public ILoggerFactory getLoggerFactory() {
        return null;
    }

    public String getLoggerFactoryClassStr() {
        return LoggerFactory.class.getName();
    }
}
