package emulate.com.google.gson.internal.reflect;

import com.github.xpenatan.gdx.backends.teavm.gen.Emulate;

/**
 * Gson's unsafe accessor skips makeAccessible, which makes field writes fail under TeaVM.
 * Pointing it back at plain reflection lets Gson mark the fields accessible.
 */
@Emulate(valueStr = "com.google.gson.internal.reflect.UnsafeReflectionAccessor", updateCode = true)
public class UnsafeReflectionAccessorEmu {

    @Emulate
    public void makeAccessible(java.lang.reflect.Field field) {
        field.setAccessible(true);
    }
}
