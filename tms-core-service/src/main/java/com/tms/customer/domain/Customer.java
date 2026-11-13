package com.tms.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * Customer master record (US-001). Only ID, name and city are defined by the story; removal is a
 * soft delete that clears {@link #isActive()}.
 */
@Entity
@Table(name = "customer")
public class Customer {

  @Id private UUID id;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 120)
  private String city;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected Customer() {}

  public Customer(String name, String city, Instant now) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.city = city;
    this.active = true;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void rename(String newName, Instant now) {
    this.name = newName;
    this.updatedAt = now;
  }

  public void relocate(String newCity, Instant now) {
    this.city = newCity;
    this.updatedAt = now;
  }

  public void deactivate(Instant now) {
    this.active = false;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCity() {
    return city;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
