package com.companya.resort;

import com.companya.resort.config.SecurityConfig;
import com.companya.resort.controller.AdminBookingController;
import com.companya.resort.entity.Role;
import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
import com.companya.resort.repository.UserRepository;
import com.companya.resort.security.JwtAccessDeniedHandler;
import com.companya.resort.security.JwtAuthenticationEntryPoint;
import com.companya.resort.security.JwtAuthenticationFilter;
import com.companya.resort.security.JwtService;
import com.companya.resort.service.AdminBookingExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AdminBookingController.class})
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
public class AdminBookingExportWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AdminBookingExportService exportService;

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
    @DisplayName("GET /api/v1/admin/bookings/export without JWT returns 401 Unauthorized")
    void testExportWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings/export")
                        .param("date", "2026-11-14"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/admin/bookings/export with missing date param returns 400 Bad Request")
    void testExportWithoutDateReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings/export")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/admin/bookings/export with invalid date format returns 400 Bad Request")
    void testExportWithInvalidDateReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings/export")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("date", "2026-11-35"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid date format. Expected YYYY-MM-DD"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/bookings/export with valid date returns 200 and CSV with attachment header")
    void testExportWithValidDateReturnsCsv() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 14);
        String mockCsv = "Booking Reference,Booking Status\r\nBK-001,CONFIRMED\r\n";
        when(exportService.exportArrivalsCsv(eq(date))).thenReturn(mockCsv);

        mockMvc.perform(get("/api/v1/admin/bookings/export")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("date", "2026-11-14"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"daily-bookings-2026-11-14.csv\""))
                .andExpect(content().string(mockCsv));
    }

    @Test
    @DisplayName("GET /api/v1/admin/bookings/export with empty arrivals returns 200 with header-only CSV")
    void testExportEmptyArrivalsReturnsHeaderOnlyCsv() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 20);
        String headerOnlyCsv = "Booking Reference,Booking Status,Guest Name,Guest Phone,Room Category Code,Room Category Name,Rate Plan Code,Rate Plan Name,Rooms Count,Adults,Children,Check-In Date,Check-Out Date,Nights,Total Amount (INR),GST Amount (INR),Payment Status\r\n";
        when(exportService.exportArrivalsCsv(eq(date))).thenReturn(headerOnlyCsv);

        mockMvc.perform(get("/api/v1/admin/bookings/export")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("date", "2026-11-20"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"daily-bookings-2026-11-20.csv\""))
                .andExpect(content().string(headerOnlyCsv));
    }
}
