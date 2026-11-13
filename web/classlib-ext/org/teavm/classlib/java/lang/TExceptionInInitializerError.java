package org.teavm.classlib.java.lang;

/** Thrown when a static initializer fails. */
public class TExceptionInInitializerError extends LinkageError {

    public TExceptionInInitializerError() {
    }

    public TExceptionInInitializerError(Throwable thrown) {
        super(String.valueOf(thrown));
        initCause(thrown);
    }

    public TExceptionInInitializerError(String detailMessage) {
        super(detailMessage);
    }
}
