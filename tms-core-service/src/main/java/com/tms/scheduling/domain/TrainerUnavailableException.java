package com.tms.scheduling.domain;

public class TrainerUnavailableException extends RuntimeException {
  public TrainerUnavailableException(String message) {
    super(message);
  }
}
