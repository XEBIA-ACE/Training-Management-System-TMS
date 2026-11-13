package com.tms.product.domain;

import java.util.UUID;

/** No active product exists with the requested ID. */
public class ProductNotFoundException extends RuntimeException {

  public ProductNotFoundException(UUID id) {
    super("Product " + id + " was not found.");
  }
}
