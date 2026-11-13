package org.teavm.classlib.java.lang.reflect;

/** A parameterized type, used by reflection based serializers. */
public interface TParameterizedType extends java.lang.reflect.Type {
    java.lang.reflect.Type[] getActualTypeArguments();

    java.lang.reflect.Type getRawType();

    java.lang.reflect.Type getOwnerType();
}
