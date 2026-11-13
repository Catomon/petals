package org.teavm.classlib.java.sql;

/** SQL date, kept simple because the game only passes it through reflection based JSON. */
public class TDate extends java.util.Date {

    public TDate() {
    }

    public TDate(long date) {
        super(date);
    }

    @Override
    public String toString() {
        return String.valueOf(getTime());
    }
}
