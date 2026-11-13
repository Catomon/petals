package emulate.java.lang;

import com.github.xpenatan.gdx.backends.teavm.gen.Emulate;

/** Runtime memory and shutdown hooks that the TeaVM class library does not provide. */
@Emulate(value = java.lang.Runtime.class, updateCode = true)
public class RuntimeMethods {

    @Emulate
    public long maxMemory() {
        return Long.MAX_VALUE;
    }

    @Emulate
    public boolean removeShutdownHook(Thread hook) {
        return false;
    }
}

