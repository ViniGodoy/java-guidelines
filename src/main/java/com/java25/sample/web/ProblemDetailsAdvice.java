package com.java25.sample.web;

import com.java25.sample.customer.CustomerNotFoundException;
import com.java25.sample.customer.CustomerStateException;
import com.java25.sample.customer.EmailAlreadyUsedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/// The single place that turns exceptions into RFC 9457 problem details.
@RestControllerAdvice
class ProblemDetailsAdvice extends ResponseEntityExceptionHandler {

  @ExceptionHandler
  ProblemDetail handle(CustomerNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler
  ProblemDetail handle(EmailAlreadyUsedException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler
  ProblemDetail handle(CustomerStateException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }
}
