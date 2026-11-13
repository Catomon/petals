package org.teavm.classlib.java.security;

/** Security providers do not exist in the browser, so this is an empty registry. */
public class TProvider {

    private final String name;

    public TProvider(String name, String version, String info) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getInfo() {
        return "";
    }

    public double getVersion() {
        return 0;
    }

    public Service getService(String type, String algorithm) {
        return null;
    }

    public java.util.Set<Service> getServices() {
        return new java.util.HashSet<>();
    }

    public static class Service {
        public String getAlgorithm() {
            return "";
        }

        public String getClassName() {
            return "";
        }

        public TProvider getProvider() {
            return null;
        }
    }
}
