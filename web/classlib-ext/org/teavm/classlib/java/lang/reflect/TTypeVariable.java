package org.teavm.classlib.java.lang.reflect;

/** A type variable, used by reflection based serializers. */
public interface TTypeVariable extends java.lang.reflect.Type {
    java.lang.reflect.Type[] getBounds();

    String getName();

    java.lang.reflect.GenericDeclaration getGenericDeclaration();
}
