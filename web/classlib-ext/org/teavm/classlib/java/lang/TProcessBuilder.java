package org.teavm.classlib.java.lang;

/** Process builder. The browser cannot start processes, so start() is unsupported. */
public final class TProcessBuilder {

    private java.util.List<String> command = new java.util.ArrayList<>();
    private boolean redirectErrorStream;

    public TProcessBuilder(java.util.List<String> command) {
        this.command = new java.util.ArrayList<>(command);
    }

    public TProcessBuilder(String... command) {
        for (String part : command) this.command.add(part);
    }

    public TProcessBuilder command(String... command) {
        this.command = new java.util.ArrayList<>();
        for (String part : command) this.command.add(part);
        return this;
    }

    public java.util.List<String> command() {
        return command;
    }

    public TProcessBuilder redirectErrorStream(boolean redirectErrorStream) {
        this.redirectErrorStream = redirectErrorStream;
        return this;
    }

    public boolean redirectErrorStream() {
        return redirectErrorStream;
    }

    public TProcess start() {
        throw new UnsupportedOperationException("Processes are not available in the browser.");
    }
}
