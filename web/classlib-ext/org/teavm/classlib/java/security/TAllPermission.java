package org.teavm.classlib.java.security;

public final class TAllPermission extends TPermission {
    public TAllPermission() {
        super("<all permissions>");
    }

    @Override
    public boolean implies(TPermission permission) {
        return true;
    }
}
