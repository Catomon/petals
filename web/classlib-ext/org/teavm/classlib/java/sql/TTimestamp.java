package org.teavm.classlib.java.sql;

/** SQL timestamp, kept simple because the game only passes it through reflection based JSON. */
public class TTimestamp extends java.util.Date {

    public TTimestamp(long time) {
        super(time);
    }

    @Override
    public String toString() {
        return String.valueOf(getTime());
    }
}
