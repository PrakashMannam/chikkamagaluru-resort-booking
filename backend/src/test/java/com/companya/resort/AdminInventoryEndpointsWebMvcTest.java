package com.companya.resort;

import com.companya.resort.config.SecurityConfig;
import com.companya.resort.controller.AdminController;
import com.companya.resort.dto.AdminDailyInventoryRateDto;
import com.companya.resort.dto.BulkUpdateInventoryRatesRequest;
import com.companya.resort.entity.Role;
import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
import com.companya.resort.exception.InvalidDateRangeException;
import com.companya.resort.exception.MissingInventoryDatesException;
import com.companya.resort.repository.UserRepository;
import com.companya.resort.security.JwtAccessDeniedHandler;
import com.companya.resort.security.JwtAuthenticationEntryPoint;
import com.companya.resort.security.JwtAuthenticationFilter;
import com.companya.resort.security.JwtService;
import com.companya.resort.service.AdminInventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        JwtService.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "cors.allowed-origin=http://localhost:5173"
})
public class AdminInventoryEndpointsWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminInventoryService adminInventoryService;

    @MockBean
    private UserRepository userRepository;

    private User staffUser;
    private String staffToken;

    @BeforeEach
    void setUp() {
        staffUser = new User(
                "reservations.demo@companya-estate.test",
                "hash",
                "Asha Patel",
                Role.STAFF,
                SpecialtyLane.ROOM,
                false
        );
        staffUser.setId(10L);

        staffToken = jwtService.generateToken(staffUser);
        when(userRepository.findByEmail("reservations.demo@companya-estate.test")).thenReturn(Optional.of(staffUser));
    }

    @Test
    @DisplayName("GET /api/v1/admin/inventory-rates without JWT returns 401 Unauthorized")
    void testGetInventoryRatesWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory-rates")
                        .param("from", "2026-11-10")
                        .param("to", "2026-11-20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/admin/inventory-rates with ROLE_STAFF returns 200 OK")
    void testGetInventoryRatesWithStaffTokenReturns200() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 10);
        AdminDailyInventoryRateDto dto = new AdminDailyInventoryRateDto(
                1L, 1L, "ROOM", "Estate Room", date,
                new BigDecimal("8500.00"), 40, 2, 64, 0L, false, null
        );

        when(adminInventoryService.getInventoryRates(date, date)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/admin/inventory-rates")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("from", "2026-11-10")
                        .param("to", "2026-11-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryCode").value("ROOM"))
                .andExpect(jsonPath("$[0].stayDate").value("2026-11-10"))
                .andExpect(jsonPath("$[0].baseRateInr").value(8500.00))
                .andExpect(jsonPath("$[0].websiteAllotmentTotal").value(40))
                .andExpect(jsonPath("$[0].bookedCount").value(2))
                .andExpect(jsonPath("$[0].totalPhysicalKeys").value(64))
                .andExpect(jsonPath("$[0].blackout").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/admin/inventory-rates with inverted dates returns 400 Bad Request")
    void testGetInventoryRatesWithInvertedRangeReturns400() throws Exception {
        when(adminInventoryService.getInventoryRates(any(), any()))
                .thenThrow(new InvalidDateRangeException("Invalid date range: 'from' must be on or before 'to'"));

        mockMvc.perform(get("/api/v1/admin/inventory-rates")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("from", "2026-11-20")
                        .param("to", "2026-11-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Date Range"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/inventory-rates/bulk without JWT returns 401 Unauthorized")
    void testBulkUpdateWithoutTokenReturns401() throws Exception {
        BulkUpdateInventoryRatesRequest request = new BulkUpdateInventoryRatesRequest(
                "ROOM", LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15),
                new BigDecimal("9500.00"), 30
        );

        mockMvc.perform(put("/api/v1/admin/inventory-rates/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/admin/inventory-rates/bulk with ROLE_STAFF returns 200 OK")
    void testBulkUpdateWithStaffTokenReturns200() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 10);
        BulkUpdateInventoryRatesRequest request = new BulkUpdateInventoryRatesRequest(
                "ROOM", date, date,
                new BigDecimal("9500.00"), 30
        );

        AdminDailyInventoryRateDto dto = new AdminDailyInventoryRateDto(
                1L, 1L, "ROOM", "Estate Room", date,
                new BigDecimal("9500.00"), 30, 2, 64, 1L, false, null
        );

        when(adminInventoryService.bulkUpdateInventoryRates(any())).thenReturn(List.of(dto));

        mockMvc.perform(put("/api/v1/admin/inventory-rates/bulk")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryCode").value("ROOM"))
                .andExpect(jsonPath("$[0].baseRateInr").value(9500.00))
                .andExpect(jsonPath("$[0].websiteAllotmentTotal").value(30));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/inventory-rates/bulk with missing dates returns 422 Unprocessable Entity")
    void testBulkUpdateMissingDatesReturns422() throws Exception {
        LocalDate date1 = LocalDate.of(2026, 11, 10);
        LocalDate date2 = LocalDate.of(2026, 11, 12);

        BulkUpdateInventoryRatesRequest request = new BulkUpdateInventoryRatesRequest(
                "ROOM", date1, date2,
                new BigDecimal("9500.00"), 30
        );

        when(adminInventoryService.bulkUpdateInventoryRates(any()))
                .thenThrow(new MissingInventoryDatesException(
                        "Missing inventory rate records for dates: [2026-11-11]",
                        List.of(LocalDate.of(2026, 11, 11))
                ));

        mockMvc.perform(put("/api/v1/admin/inventory-rates/bulk")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.missingDates[0]").value("2026-11-11"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/inventory-rates/bulk with invalid allotment returns 400 Bad Request")
    void testBulkUpdateInvalidAllotmentReturns400() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 10);
        BulkUpdateInventoryRatesRequest request = new BulkUpdateInventoryRatesRequest(
                "ROOM", date, date,
                new BigDecimal("9500.00"), 2
        );

        when(adminInventoryService.bulkUpdateInventoryRates(any()))
                .thenThrow(new IllegalArgumentException("Website allotment total (2) cannot be less than booked count (5) on 2026-11-10"));

        mockMvc.perform(put("/api/v1/admin/inventory-rates/bulk")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Website allotment total (2) cannot be less than booked count (5) on 2026-11-10"));
    }
}
