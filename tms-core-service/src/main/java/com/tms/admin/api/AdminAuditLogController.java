package com.tms.admin.api;

import com.tms.crosscutting.audit.AuditLogClient;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin views over the immutable audit store (US-004, FR-US-004-AC5-003/AC6-003). Access is
 * restricted to {@code SUPER_ADMIN} by SecurityConfig ({@code /api/v1/admin/**}).
 *
 * <p>GAP(FR-US-004-AC5-003/AC6-003): the story defines no pagination, filtering or retention. This
 * stopgap pages newest first ({@code page}, {@code size} max {@value #MAX_PAGE_SIZE}), applies no
 * filters and no retention; {@code audit-logs} returns every entry except UNAUTHORIZED_ACCESS,
 * which is served by {@code unauthorized-access-logs}. Pending product confirmation.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminAuditLogController {

  static final int DEFAULT_PAGE_SIZE = 20;
  static final int MAX_PAGE_SIZE = 100;

  private final AuditLogClient auditLogClient;

  public AdminAuditLogController(AuditLogClient auditLogClient) {
    this.auditLogClient = auditLogClient;
  }

  @GetMapping("/audit-logs")
  public AuditLogPage auditLogs(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
    return AuditLogPage.from(auditLogClient.findPage(false, page, size));
  }

  @GetMapping("/unauthorized-access-logs")
  public AuditLogPage unauthorizedAccessLogs(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) @Min(1) @Max(MAX_PAGE_SIZE) int size) {
    return AuditLogPage.from(auditLogClient.findPage(true, page, size));
  }
}
