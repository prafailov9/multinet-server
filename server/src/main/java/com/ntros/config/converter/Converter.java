package com.ntros.config.converter;

public interface Converter<F, M> {

  F toFileObject(M modelObject);

  M toModelObject(F fileObject);

}
