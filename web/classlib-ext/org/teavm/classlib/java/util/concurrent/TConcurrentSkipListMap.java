package org.teavm.classlib.java.util.concurrent;

/** Sorted concurrent map, backed by a plain sorted map because the browser is single threaded. */
public class TConcurrentSkipListMap<K, V> extends java.util.TreeMap<K, V>
        implements java.util.concurrent.ConcurrentNavigableMap<K, V> {

    @Override
    public java.util.concurrent.ConcurrentNavigableMap<K, V> descendingMap() {
        return this;
    }

    @Override
    public java.util.concurrent.ConcurrentNavigableMap<K, V> subMap(K fromKey, K toKey) {
        return this;
    }

    @Override
    public java.util.concurrent.ConcurrentNavigableMap<K, V> headMap(K toKey) {
        return this;
    }

    @Override
    public java.util.concurrent.ConcurrentNavigableMap<K, V> tailMap(K fromKey) {
        return this;
    }

    @Override
    public java.util.concurrent.ConcurrentNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return this;
    }

    @Override
    public java.util.concurrent.ConcurrentNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return this;
    }

    @Override
    public java.util.concurrent.ConcurrentNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return this;
    }

    @Override
    public java.util.NavigableSet<K> keySet() {
        return navigableKeySet();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        V current = get(key);
        if (current == null) put(key, value);
        return current;
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (java.util.Objects.equals(get(key), value)) {
            remove(key);
            return true;
        }
        return false;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        if (java.util.Objects.equals(get(key), oldValue)) {
            put(key, newValue);
            return true;
        }
        return false;
    }

    @Override
    public V replace(K key, V value) {
        if (containsKey(key)) {
            return put(key, value);
        }
        return null;
    }
}
