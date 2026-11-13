package emulate.java.lang;

import com.github.xpenatan.gdx.backends.teavm.gen.Emulate;

/** Thread context class loaders are not a thing in the browser. */
@Emulate(value = java.lang.Thread.class, updateCode = true)
public class ThreadMethods {

    @Emulate
    public void setContextClassLoader(ClassLoader classLoader) {
    }
}

