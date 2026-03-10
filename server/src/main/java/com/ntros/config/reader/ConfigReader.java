package com.ntros.config.reader;

import java.util.List;

public interface ConfigReader<T> {


  T read();
  List<T> readAll();


}
