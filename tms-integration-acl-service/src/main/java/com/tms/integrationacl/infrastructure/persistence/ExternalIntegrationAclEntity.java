package com.tms.integrationacl.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

/**
 * C4: JPA representation of {@code ExternalIntegrationAclAggregate}, with optimistic locking via
 * {@code version} and soft delete via {@code deleted}.
 *
 * <p>TODO SCAFFOLD-SVC-003: table name, columns and constraints are not specified in the export;
 * only the identifier, version and soft-delete flag named by the architecture are declared.
 */
@Entity
@Table(name = "external_integration_acl")
public class ExternalIntegrationAclEntity {

  @Id private UUID id;

  @Version private Long version;

  @Column(nullable = false)
  private boolean deleted;

  protected ExternalIntegrationAclEntity() {}

  public UUID getId() {
    return id;
  }

  public Long getVersion() {
    return version;
  }

  public boolean isDeleted() {
    return deleted;
  }
}
