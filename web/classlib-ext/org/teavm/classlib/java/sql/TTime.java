package org.teavm.classlib.java.sql;

/** SQL time, kept simple because the game only passes it through reflection based JSON. */
public class TTime extends java.util.Date {

    public TTime(long time) {
        super(time);
    }

    @Override
    public String toString() {
        return String.valueOf(getTime());
    }
}
