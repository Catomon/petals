package org.teavm.classlib.java.lang.reflect;

/** An array of a generic type, used by reflection based serializers. */
public interface TGenericArrayType extends java.lang.reflect.Type {
    java.lang.reflect.Type getGenericComponentType();
}
