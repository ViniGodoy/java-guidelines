package com.java25.sample.customer;

import java.io.Serial;

/// Thrown when another customer already uses the email address.
public class EmailAlreadyUsedException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public EmailAlreadyUsedException(String email) {
    super("Email already in use: " + email);
  }
}
