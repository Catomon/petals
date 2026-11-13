package emulate.java.lang;

import com.github.xpenatan.gdx.backends.teavm.gen.Emulate;

import java.net.URL;
import java.util.Enumeration;

/** Resource and class loading hooks that the TeaVM class library does not provide. */
@Emulate(value = java.lang.ClassLoader.class, updateCode = true)
public class ClassLoaderMethods {

    @Emulate
    public static Enumeration<URL> getSystemResources(String name) {
        return null;
    }

    @Emulate
    public Enumeration<URL> getResources(String name) {
        return null;
    }

    @Emulate
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return Class.forName(name);
    }
}

