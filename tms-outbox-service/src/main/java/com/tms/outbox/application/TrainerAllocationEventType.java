package com.tms.outbox.application;

import java.util.Locale;

/**
 * US-001: event categories of trainer allocation notifications published by the Outbox Processing
 * Service (FR-US-001-009 "by event type (allocation vs deallocation)").
 */
public enum TrainerAllocationEventType {
  ALLOCATION,
  DEALLOCATION;

  /** Lower-case key used in the stats API ({@code countsByEventType}) and metric tags. */
  public String key() {
    return name().toLowerCase(Locale.ROOT);
  }
}
