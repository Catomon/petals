package emulate.java.lang;

import com.github.xpenatan.gdx.backends.teavm.gen.Emulate;

/** Process termination, which the browser cannot do, so it only stops the current application. */
@Emulate(value = java.lang.System.class, updateCode = true)
public class SystemMethods {

    @Emulate
    public static void exit(int status) {
    }
}

