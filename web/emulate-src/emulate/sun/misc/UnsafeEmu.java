package emulate.sun.misc;

import com.github.xpenatan.gdx.backends.teavm.gen.Emulate;

import java.lang.reflect.Field;

/**
 * Gson probes sun.misc.Unsafe for reflective access. The browser has no such thing, and the JDK
 * version drags in native methods, so this replacement exists only so Gson can fall back to its
 * plain reflection path.
 */
@Emulate(value = sun.misc.Unsafe.class)
public class UnsafeEmu {

    public static final Object theUnsafe = null;

    public int arrayBaseOffset(Class<?> type) {
        return 0;
    }

    public int arrayIndexScale(Class<?> type) {
        return 1;
    }

    public long objectFieldOffset(Field field) {
        return 0;
    }

    public long staticFieldOffset(Field field) {
        return 0;
    }

    public Object staticFieldBase(Field field) {
        return null;
    }

    public Object allocateInstance(Class<?> type) throws InstantiationException {
        throw new InstantiationException("Unsafe is not available in the browser.");
    }

    public Object getObject(Object target, long offset) {
        return null;
    }

    public void putObject(Object target, long offset, Object value) {
    }

    public int getInt(Object target, long offset) {
        return 0;
    }

    public void putInt(Object target, long offset, int value) {
    }

    public long getLong(Object target, long offset) {
        return 0;
    }

    public void putLong(Object target, long offset, long value) {
    }

    public boolean compareAndSwapObject(Object target, long offset, Object expected, Object value) {
        return false;
    }

    public boolean compareAndSwapInt(Object target, long offset, int expected, int value) {
        return false;
    }

    public boolean compareAndSwapLong(Object target, long offset, long expected, long value) {
        return false;
    }

    public void ensureClassInitialized(Class<?> type) {
    }

    public void throwException(Throwable throwable) {
        throw new RuntimeException(throwable);
    }
}
