package org.teavm.classlib.java.io;

/** Object streams are not available in the browser, the class only has to link. */
public class TObjectInputStream {

    public TObjectInputStream() {
    }

    public Object readObject() throws java.io.IOException, ClassNotFoundException {
        throw new UnsupportedOperationException("Object streams are not available in the browser.");
    }

    public void defaultReadObject() throws java.io.IOException, ClassNotFoundException {
        throw new UnsupportedOperationException("Object streams are not available in the browser.");
    }

    public void close() {
    }
}
