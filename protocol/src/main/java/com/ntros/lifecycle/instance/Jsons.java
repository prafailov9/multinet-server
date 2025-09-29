package com.ntros.lifecycle.instance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class Jsons {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

  private Jsons() {
  }

  public static ObjectMapper mapper() {
    return MAPPER;
  }
}