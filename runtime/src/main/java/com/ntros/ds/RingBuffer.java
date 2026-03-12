package com.ntros.ds;

public interface RingBuffer<E> {

  boolean offer(E obj);
  E poll();
  E peek();
  E element();
  int size();
  boolean isEmpty();
  boolean isFull();
  int capacity();


}
