package com.tms.outbox.infrastructure;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * C3 Transaction Manager: explicit transactional boundaries for outbox operations (claiming a
 * batch, recording delivery results). Named to avoid clashing with Spring's {@code
 * transactionManager} bean.
 */
@Component
public class OutboxTransactionManager {

  private final TransactionTemplate transactionTemplate;

  public OutboxTransactionManager(PlatformTransactionManager transactionManager) {
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public <T> T inTransaction(Supplier<T> work) {
    return transactionTemplate.execute(status -> work.get());
  }
}
