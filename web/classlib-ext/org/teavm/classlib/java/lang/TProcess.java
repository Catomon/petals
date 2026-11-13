package org.teavm.classlib.java.lang;

/** Process handle. The browser cannot start processes, so nothing here does work. */
public abstract class TProcess extends Object {

    public abstract java.io.OutputStream getOutputStream();

    public abstract java.io.InputStream getInputStream();

    public abstract java.io.InputStream getErrorStream();

    public abstract int waitFor() throws InterruptedException;

    public boolean waitFor(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
        return true;
    }

    public abstract int exitValue();

    public abstract void destroy();

    public TProcess destroyForcibly() {
        destroy();
        return this;
    }

    public boolean isAlive() {
        return false;
    }
}
