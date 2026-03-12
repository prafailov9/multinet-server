package com.ntros.model.world.engine.gameoflife.fast;

public final class BitGrid {

  private final int width;
  private final int height;
  private final int stride;
  private final long[] data;

  public BitGrid(int width, int height) {
    this.width = width;
    this.height = height;
    this.stride = (width + 63) >>> 6;
    this.data = new long[height * stride];
  }

  public boolean get(int x, int y) {
    int idx = y * stride + (x >>> 6);
    long mask = 1L << (x & 63);
    return (data[idx] & mask) != 0;
  }

  public void set(int x, int y) {
    int idx = y * stride + (x >>> 6);
    data[idx] |= 1L << (x & 63);
  }

  public void clear(int x, int y) {
    int idx = y * stride + (x >>> 6);
    data[idx] &= ~(1L << (x & 63));
  }

  public void clearAll() {
    java.util.Arrays.fill(data, 0);
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  public int stride() {
    return stride;
  }

  public long[] raw() {
    return data;
  }

  public void copyFrom(BitGrid other) {
    System.arraycopy(other.data, 0, this.data, 0, data.length);
  }
}