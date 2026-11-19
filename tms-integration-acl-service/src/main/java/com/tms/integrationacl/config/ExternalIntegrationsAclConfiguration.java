package com.tms.integrationacl.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables binding of {@link ExternalIntegrationsAclProperties}. */
@Configuration
@EnableConfigurationProperties(ExternalIntegrationsAclProperties.class)
public class ExternalIntegrationsAclConfiguration {}
