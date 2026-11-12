package com.tms.outbox.infrastructure.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

/**
 * Verifies connectivity to the PostgreSQL database holding the outbox table. The {@link DataSource}
 * is auto-configured from {@code TMS_OUTBOX_DB_URL}, {@code TMS_OUTBOX_DB_USERNAME}, {@code
 * TMS_OUTBOX_DB_PASSWORD} and {@code TMS_OUTBOX_DB_SCHEMA}.
 */
@Component
public class DatabaseConnectionHelper {

  private static final int VALIDATION_TIMEOUT_SECONDS = 5;

  private final DataSource dataSource;

  public DatabaseConnectionHelper(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public DatabaseConnectionInfo verifyConnection() {
    try (Connection connection = dataSource.getConnection()) {
      if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
        throw new IllegalStateException("Outbox database connection is not valid");
      }
      DatabaseMetaData metaData = connection.getMetaData();
      return new DatabaseConnectionInfo(
          metaData.getDatabaseProductName(),
          metaData.getDatabaseProductVersion(),
          connection.getSchema());
    } catch (SQLException e) {
      throw new IllegalStateException("Unable to connect to the outbox database", e);
    }
  }

  public record DatabaseConnectionInfo(String productName, String productVersion, String schema) {}
}
