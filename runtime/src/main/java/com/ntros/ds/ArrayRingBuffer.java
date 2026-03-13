package com.ntros.ds;

/**
 * Single Prod single cons buffer
 */
public class ArrayRingBuffer<E> implements RingBuffer<E> {

  private final int mask;
  private final Object[] buf;

  // Padding prevents false sharing: head and tail on separate cache lines.
  // Without it, the CPU treats them as one unit and forces cache coherence
  // traffic between producer and consumer cores — killing the whole point.
  private volatile long head = 0;   // written only by producer

  private volatile long tail = 0;   // written only by consumer

  public ArrayRingBuffer(int capacity) {  // capacity must be power of 2
    mask = capacity - 1;
    buf = new Object[capacity];
  }

  @Override
  public boolean offer(E item) {
    long h = head;
    if (h - tail >= buf.length) return false; // full
    buf[(int) (h & mask)] = item;
    head = h + 1;                        // volatile write = release barrier
    return true;
  }

  @SuppressWarnings("unchecked")
  public E poll() {
    long t = tail;
    if (t == head) {
      return null;          // empty
    }
    E item = (E) buf[(int) (t & mask)];
    tail = t + 1;                        // volatile write = release barrier
    return item;
  }

  // consumer only
  @Override
  @SuppressWarnings("unchecked")
  public E peek() {
    if (tail == head) {
      return null;
    }
    return (E) buf[(int) (tail & mask)];
  }

  @Override
  @SuppressWarnings("unchecked")
  public E element() {
    if (head == tail) {
      return null;
    }
    return (E) buf[(int) (tail & mask)];
  }

  @Override
  public boolean isEmpty() {
    return head == tail;
  }

  @Override
  public boolean isFull() {
    return head - tail > mask;
  }

  @Override
  public int capacity() {
    return buf.length;
  }

  @Override
  public int size() {
    return (int) (head - tail);
  }
}