package com.ntros.ds;

/**
 * Single Prod single cons buffer
 */
public class ArrayRingBuffer<E> implements RingBuffer<E> {

  private final Object[] buffer;
  private final int mask;

  // written only by producer
  private volatile int head = 0;

  // written only by consumer
  private volatile int tail = 0;

  public ArrayRingBuffer(int capacity) {
    if (Integer.bitCount(capacity) != 1) {
      throw new IllegalArgumentException("Capacity must be a power of 2");
    }

    this.buffer = new Object[capacity];
    this.mask = capacity - 1;
  }

  // producer only
  @Override
  public boolean offer(E item) {
    int nextHead = (head + 1) & mask;

    if (nextHead == tail) {
      return false; // buffer full
    }

    buffer[head] = item;
    head = nextHead;
    return true;
  }

  // consumer only
  @Override
  @SuppressWarnings("unchecked")
  public E poll() {
    if (tail == head) {
      return null; // buffer empty
    }

    int index = tail;
    E item = (E) buffer[index];

    buffer[index] = null; // avoid memory leak
    tail = (tail + 1) & mask;

    return item;
  }

  // consumer only
  @Override
  @SuppressWarnings("unchecked")
  public E peek() {
    if (tail == head) {
      return null;
    }
    return (E) buffer[tail];
  }

  @Override
  @SuppressWarnings("unchecked")
  public E element() {
    if (head == tail) {
      return null;
    }
    return (E) buffer[head];
  }

  @Override
  public boolean isEmpty() {
    return head == tail;
  }

  @Override
  public boolean isFull() {
    return ((head + 1) & mask) == tail;
  }

  @Override
  public int capacity() {
    return buffer.length;
  }

  @Override
  public int size() {
    return (head - tail) & mask;
  }
}