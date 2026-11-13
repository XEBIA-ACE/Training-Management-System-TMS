package com.tms.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tms.crosscutting.monitoring.MonitoringComponent;
import com.tms.trainer.api.TrainerController;
import com.tms.trainer.application.TrainerSearchService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.event.Level;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * [US-002-ac-7] / FR-US-002-007: the controller emits one structured event per search with user,
 * timestamp (added by the monitoring component) and high-level filters, never the free text.
 */
class TrainerSearchLoggingTest {

  private TrainerSearchService service;
  private MonitoringComponent monitoring;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    service = mock(TrainerSearchService.class);
    monitoring = mock(MonitoringComponent.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new TrainerController(service, monitoring)).build();
  }

  @Test
  @DisplayName("[US-002-ac-7] completed search logs user and filters but omits the free-text term")
  void us002Ac7_logsUserAndFiltersWithoutFreeText() throws Exception {
    when(service.searchTrainers(any(), eq(0), eq(20))).thenReturn(new PageImpl<>(List.of()));

    mockMvc
        .perform(
            get("/api/v1/trainers?status=ACTIVE&location=Pune&q=secret-person@example.com")
                .principal(() -> "alice"))
        .andExpect(status().isOk());

    Map<String, Object> fields = capturedFields(Level.INFO);
    assertThat(fields)
        .containsEntry("user", "alice")
        .containsEntry("outcome", "SUCCESS")
        .containsEntry("filter.location", "Pune")
        .containsEntry("filter.textTermProvided", true);
    assertThat(fields.get("filter.status")).hasToString("ACTIVE");
    // TODO(FR-US-002-007): omission is provisional; the free text must appear nowhere.
    assertThat(fields.toString()).doesNotContain("secret-person").doesNotContain("example.com");
  }

  @Test
  @DisplayName("[US-002-ac-7] a failed search is logged too, and the failure still propagates")
  void us002Ac7_failedSearchIsLogged() throws Exception {
    when(service.searchTrainers(any(), eq(0), eq(20))).thenThrow(new IllegalStateException("db down"));

    try {
      mockMvc.perform(get("/api/v1/trainers?q=hidden").principal(() -> "bob"));
    } catch (Exception expected) {
      assertThat(expected.getCause()).isInstanceOf(IllegalStateException.class);
    }

    Map<String, Object> fields = capturedFields(Level.WARN);
    assertThat(fields).containsEntry("user", "bob").containsEntry("outcome", "FAILURE");
    assertThat(fields.toString()).doesNotContain("hidden");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> capturedFields(Level level) {
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(monitoring).logEvent(eq(level), eq(TrainerController.SEARCH_EVENT), captor.capture());
    return captor.getValue();
  }
}
