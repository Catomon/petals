package org.teavm.classlib.java.util.concurrent;

public class TLinkedBlockingQueue<E> extends java.util.AbstractQueue<E> {
    private final java.util.ArrayList<E> items = new java.util.ArrayList<>();

    public TLinkedBlockingQueue() {
    }

    public TLinkedBlockingQueue(int capacity) {
    }

    @Override
    public boolean offer(E e) {
        return items.add(e);
    }

    @Override
    public boolean add(E e) {
        return items.add(e);
    }

    public void put(E e) {
        items.add(e);
    }

    @Override
    public E poll() {
        return items.isEmpty() ? null : items.remove(0);
    }

    public E take() throws InterruptedException {
        return poll();
    }

    @Override
    public E peek() {
        return items.isEmpty() ? null : items.get(0);
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public java.util.Iterator<E> iterator() {
        return items.iterator();
    }
}
