package emulate.java.lang.reflect;

import com.github.xpenatan.gdx.backends.teavm.gen.Emulate;

/** Extra field reflection methods that the TeaVM class library does not provide. */
@Emulate(value = java.lang.reflect.Field.class, updateCode = true)
public class FieldMethods {

    /** Generic signatures are not kept in the compiled program, so the raw type is all we have. */
    @Emulate
    public java.lang.reflect.Type getGenericType() {
        return java.lang.reflect.Field.class.cast(this).getType();
    }
}
