package org.teavm.classlib.java.security;

public abstract class TPermission {
    private final String name;

    protected TPermission(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract boolean implies(TPermission permission);
}
