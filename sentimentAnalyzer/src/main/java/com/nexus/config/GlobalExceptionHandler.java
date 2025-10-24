package com.nexus.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public final class GlobalExceptionHandler {
  /**
  * Handles IllegalArgumentException and returns
  * the exception message with NOT_FOUND status.
   *
   * @param e the IllegalArgumentException thrown
   * @return the exception message
   */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public String handleNotFound(final IllegalArgumentException e) {
    return e.getMessage();
  }

  /**
   * Handles general exceptions and returns a generic error
   * message with INTERNAL_SERVER_ERROR status.
   *
   * @param e the Exception thrown
   * @return a generic error message with the exception details
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public String handleGeneralError(final Exception e) {
    return "An error occurred: "
      + e.getMessage();
  }
}
