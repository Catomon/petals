package emulate.java.lang;

import com.github.xpenatan.gdx.backends.teavm.gen.Emulate;

/** Extra reflection methods that the TeaVM class library does not provide. */
@Emulate(value = java.lang.Class.class, updateCode = true)
public class ClassMethods {

    @Emulate
    public boolean isRecord() {
        return false;
    }

    @Emulate
    public boolean isHidden() {
        return false;
    }

    @Emulate
    public boolean isAnonymousClass() {
        return false;
    }

    @Emulate
    public java.lang.reflect.Type getGenericSuperclass() {
        return Object.class;
    }

    @Emulate
    public java.lang.reflect.Type[] getGenericInterfaces() {
        return new java.lang.reflect.Type[0];
    }

    @Emulate
    public java.lang.reflect.TypeVariable[] getTypeParameters() {
        return new java.lang.reflect.TypeVariable[0];
    }

    @Emulate
    public java.net.URL getResource(String name) {
        return null;
    }
}
