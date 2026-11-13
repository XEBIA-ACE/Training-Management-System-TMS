package com.tms.outbox.application;

import com.tms.outbox.config.TrainerNotificationProperties;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * US-001: maps outbox {@code eventType} values to {@link TrainerAllocationEventType} using {@link
 * TrainerNotificationProperties}. Event types are matched exactly (case-sensitive), as stored.
 */
@Component
public class TrainerAllocationEventClassifier {

  private final Map<String, TrainerAllocationEventType> byEventType;

  public TrainerAllocationEventClassifier(TrainerNotificationProperties properties) {
    Map<String, TrainerAllocationEventType> mapping = new LinkedHashMap<>();
    register(mapping, properties.allocationEventTypes(), TrainerAllocationEventType.ALLOCATION);
    register(mapping, properties.deallocationEventTypes(), TrainerAllocationEventType.DEALLOCATION);
    this.byEventType = Collections.unmodifiableMap(mapping);
  }

  private static void register(
      Map<String, TrainerAllocationEventType> mapping,
      Iterable<String> eventTypes,
      TrainerAllocationEventType category) {
    if (eventTypes == null) {
      return;
    }
    for (String eventType : eventTypes) {
      if (!StringUtils.hasText(eventType)) {
        continue;
      }
      TrainerAllocationEventType previous = mapping.putIfAbsent(eventType.trim(), category);
      if (previous != null && previous != category) {
        throw new IllegalStateException(
            "Outbox event type "
                + eventType
                + " is configured as both allocation and deallocation");
      }
    }
  }

  public Optional<TrainerAllocationEventType> classify(String eventType) {
    return eventType == null ? Optional.empty() : Optional.ofNullable(byEventType.get(eventType));
  }

  public Set<String> eventTypes() {
    return byEventType.keySet();
  }
}
