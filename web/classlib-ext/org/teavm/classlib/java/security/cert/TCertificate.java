package org.teavm.classlib.java.security.cert;

public interface TCertificate {
    byte[] getEncoded();

    String getType();

    java.security.PublicKey getPublicKey();
}
