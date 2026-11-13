package org.teavm.classlib.java.lang.reflect;

/** A wildcard type, used by reflection based serializers. */
public interface TWildcardType extends java.lang.reflect.Type {
    java.lang.reflect.Type[] getLowerBounds();

    java.lang.reflect.Type[] getUpperBounds();
}
