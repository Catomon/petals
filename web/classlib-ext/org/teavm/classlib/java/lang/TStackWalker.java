package org.teavm.classlib.java.lang;

/** Stack walking is not supported by the JavaScript runtime. */
public final class TStackWalker {

    private static final TStackWalker INSTANCE = new TStackWalker();

    private TStackWalker() {
    }

    public static TStackWalker getInstance() {
        return INSTANCE;
    }
}
