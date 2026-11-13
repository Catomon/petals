package org.teavm.classlib.java.util.concurrent.atomic;

public class TAtomicIntegerArray {
    private final int[] array;

    public TAtomicIntegerArray(int length) {
        array = new int[length];
    }

    public TAtomicIntegerArray(int[] array) {
        this.array = array.clone();
    }

    public final int length() {
        return array.length;
    }

    public final int get(int i) {
        return array[i];
    }

    public final void set(int i, int newValue) {
        array[i] = newValue;
    }

    public final void lazySet(int i, int newValue) {
        array[i] = newValue;
    }

    public final int getAndSet(int i, int newValue) {
        int old = array[i];
        array[i] = newValue;
        return old;
    }

    public final boolean compareAndSet(int i, int expect, int update) {
        if (array[i] == expect) {
            array[i] = update;
            return true;
        }
        return false;
    }

    public final int getAndIncrement(int i) {
        int old = array[i];
        array[i] = old + 1;
        return old;
    }

    public final int getAndDecrement(int i) {
        int old = array[i];
        array[i] = old - 1;
        return old;
    }

    public final int getAndAdd(int i, int delta) {
        int old = array[i];
        array[i] = old + delta;
        return old;
    }

    public final int incrementAndGet(int i) {
        array[i] = array[i] + 1;
        return array[i];
    }

    public final int decrementAndGet(int i) {
        array[i] = array[i] - 1;
        return array[i];
    }

    public final int addAndGet(int i, int delta) {
        array[i] = array[i] + delta;
        return array[i];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(array[i]);
        }
        return sb.append("]").toString();
    }
}
