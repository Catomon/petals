package emulate.java.util;

import com.github.xpenatan.gdx.backends.teavm.gen.Emulate;

/** Timer.purge has no scheduled tasks to remove in the browser. */
@Emulate(value = java.util.Timer.class, updateCode = true)
public class TimerMethods {

    @Emulate
    public int purge() {
        return 0;
    }
}

