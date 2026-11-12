package com.tms.crosscutting.cache;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring Cache. The provider is selected by {@code TMS_CACHE_TYPE} (default {@code redis},
 * connection from {@code TMS_REDIS_URL}); TTL from {@code TMS_CACHE_DEFAULT_TTL}.
 */
@Configuration
@EnableCaching
public class CacheConfig {}
