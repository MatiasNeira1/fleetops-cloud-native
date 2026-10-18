package com.fleetops.alerta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetops.alerta.api.controller.AlertController;
import com.fleetops.alerta.api.error.ApiExceptionHandler;
import com.fleetops.alerta.application.service.AlertNotFoundException;
import com.fleetops.alerta.application.service.AlertService;
import com.fleetops.alerta.domain.model.AlertSeverity;
import com.fleetops.alerta.domain.model.AlertStatus;
import com.fleetops.alerta.domain.model.AlertType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class AlertControllerTests {
    private final AlertService service = mock(AlertService.class);
    private final MockMvc mvc = mvc();

    private MockMvc mvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(new AlertController(service))
            .setControllerAdvice(new ApiExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setValidator(validator).build();
    }

    @Test
    void acceptsFiltersAndPagination() throws Exception {
        when(service.list(eq("trip-1"), eq("vehicle-1"), eq(AlertType.LOW_FUEL),
            eq(AlertSeverity.CRITICAL), eq(AlertStatus.ACTIVE), any(Pageable.class)))
            .thenReturn(Page.empty(PageRequest.of(1, 5)));
        mvc.perform(get("/api/v1/alerts").param("tripId", "trip-1")
            .param("vehicleId", "vehicle-1").param("type", "LOW_FUEL")
            .param("severity", "CRITICAL").param("status", "ACTIVE")
            .param("page", "1").param("size", "5"))
            .andExpect(status().isOk());
        ArgumentCaptor<Pageable> requestedPage = ArgumentCaptor.forClass(Pageable.class);
        verify(service).list(eq("trip-1"), eq("vehicle-1"), eq(AlertType.LOW_FUEL),
            eq(AlertSeverity.CRITICAL), eq(AlertStatus.ACTIVE), requestedPage.capture());
        assertEquals(1, requestedPage.getValue().getPageNumber());
        assertEquals(5, requestedPage.getValue().getPageSize());
    }

    @Test
    void missingAlertReturns404ProblemDetail() throws Exception {
        when(service.get(999L)).thenThrow(new AlertNotFoundException(999L));
        mvc.perform(get("/api/v1/alerts/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void rejectsMissingStatus() throws Exception {
        mvc.perform(patch("/api/v1/alerts/1/status")
            .contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
    }
}
