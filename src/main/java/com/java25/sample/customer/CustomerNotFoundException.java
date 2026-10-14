package com.java25.sample.customer;

import java.io.Serial;

/// Thrown when no customer exists with the requested id.
public class CustomerNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public CustomerNotFoundException(long id) {
    super("No customer with id " + id);
  }
}
