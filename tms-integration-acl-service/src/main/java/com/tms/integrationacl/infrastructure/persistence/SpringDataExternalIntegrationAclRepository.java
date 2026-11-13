package com.tms.integrationacl.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** C4: Spring Data JPA repository for {@link ExternalIntegrationAclEntity}. */
public interface SpringDataExternalIntegrationAclRepository
    extends JpaRepository<ExternalIntegrationAclEntity, UUID> {}
