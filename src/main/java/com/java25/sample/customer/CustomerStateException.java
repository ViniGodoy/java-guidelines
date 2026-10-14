package com.java25.sample.customer;

import java.io.Serial;

/// Thrown when a customer state transition is not allowed.
public class CustomerStateException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public CustomerStateException(String message) {
    super(message);
  }
}
