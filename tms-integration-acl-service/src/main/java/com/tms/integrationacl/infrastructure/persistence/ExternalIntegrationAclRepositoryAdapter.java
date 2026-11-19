package com.tms.integrationacl.infrastructure.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * C4: persistence port adapter translating between {@code ExternalIntegrationAclAggregate} and
 * {@link ExternalIntegrationAclEntity} ({@code load}, {@code search}). Only active when persistence
 * is enabled (profile {@code persistence}).
 *
 * <p>TODO SCAFFOLD-SVC-003: skeleton only; load/search, ExternalIntegrationAclEntityMapper and the
 * QueryDSL search belong to a later story.
 */
@Component
@ConditionalOnProperty(prefix = "tms.integration-acl.persistence", name = "enabled")
public class ExternalIntegrationAclRepositoryAdapter {

  private final SpringDataExternalIntegrationAclRepository repository;

  public ExternalIntegrationAclRepositoryAdapter(
      SpringDataExternalIntegrationAclRepository repository) {
    this.repository = repository;
  }
}
