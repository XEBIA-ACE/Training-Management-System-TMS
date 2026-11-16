package com.tms.scheduling.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkshopRepository extends JpaRepository<Workshop, UUID> {}
