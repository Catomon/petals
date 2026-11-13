package org.teavm.classlib.java.util.concurrent.atomic;

public class TAtomicLongArray {
    private final long[] array;

    public TAtomicLongArray(int length) {
        array = new long[length];
    }

    public TAtomicLongArray(long[] array) {
        this.array = array.clone();
    }

    public final int length() {
        return array.length;
    }

    public final long get(int i) {
        return array[i];
    }

    public final void set(int i, long newValue) {
        array[i] = newValue;
    }

    public final void lazySet(int i, long newValue) {
        array[i] = newValue;
    }

    public final long getAndSet(int i, long newValue) {
        long old = array[i];
        array[i] = newValue;
        return old;
    }

    public final boolean compareAndSet(int i, long expect, long update) {
        if (array[i] == expect) {
            array[i] = update;
            return true;
        }
        return false;
    }

    public final long getAndIncrement(int i) {
        long old = array[i];
        array[i] = old + 1;
        return old;
    }

    public final long getAndDecrement(int i) {
        long old = array[i];
        array[i] = old - 1;
        return old;
    }

    public final long getAndAdd(int i, long delta) {
        long old = array[i];
        array[i] = old + delta;
        return old;
    }

    public final long incrementAndGet(int i) {
        array[i] = array[i] + 1;
        return array[i];
    }

    public final long decrementAndGet(int i) {
        array[i] = array[i] - 1;
        return array[i];
    }

    public final long addAndGet(int i, long delta) {
        array[i] = array[i] + delta;
        return array[i];
    }
}
