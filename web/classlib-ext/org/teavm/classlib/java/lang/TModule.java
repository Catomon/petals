package org.teavm.classlib.java.lang;

/** A named module, enough for libraries that ask whether one exists. */
public final class TModule {

    private final String name;

    public TModule(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isNamed() {
        return name != null;
    }

    @Override
    public String toString() {
        return name == null ? "unnamed module" : "module " + name;
    }
}
