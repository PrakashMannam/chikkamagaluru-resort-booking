package com.companya.resort;

import com.companya.resort.config.SecurityConfig;
import com.companya.resort.controller.AdminBlackoutController;
import com.companya.resort.dto.AdminBlackoutDto;
import com.companya.resort.dto.CreateBlackoutRequest;
import com.companya.resort.entity.Role;
import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
import com.companya.resort.exception.BlackoutDateConflictException;
import com.companya.resort.repository.UserRepository;
import com.companya.resort.security.JwtAccessDeniedHandler;
import com.companya.resort.security.JwtAuthenticationEntryPoint;
import com.companya.resort.security.JwtAuthenticationFilter;
import com.companya.resort.security.JwtService;
import com.companya.resort.service.AdminBlackoutPromoService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminBlackoutController.class})
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
public class AdminBlackoutEndpointsWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminBlackoutPromoService service;

    @MockBean
    private UserRepository userRepository;

    private User staffUser;
    private String staffToken;

    @BeforeEach
    void setUp() {
        staffUser = new User(
                "rooms.demo@companya-estate.test",
                "hash",
                "Ananya Hegde",
                Role.STAFF,
                SpecialtyLane.ROOM,
                false
        );
        staffUser.setId(10L);

        staffToken = jwtService.generateToken(staffUser);
        when(userRepository.findByEmail("rooms.demo@companya-estate.test")).thenReturn(Optional.of(staffUser));
    }

    @Test
    @DisplayName("GET /api/v1/admin/blackouts without token returns 401 Unauthorized")
    void testGetBlackoutsWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/blackouts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/admin/blackouts with ROLE_STAFF returns 200 OK")
    void testGetBlackoutsWithStaffTokenReturns200() throws Exception {
        LocalDate date = LocalDate.of(2026, 12, 24);
        AdminBlackoutDto dto = new AdminBlackoutDto(1L, null, null, "All Resort", date, "Christmas Buyout");

        when(service.getBlackouts(null, null)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/admin/blackouts")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].roomCategoryName").value("All Resort"))
                .andExpect(jsonPath("$[0].blackoutDate").value("2026-12-24"))
                .andExpect(jsonPath("$[0].reason").value("Christmas Buyout"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/blackouts creates a blackout and returns 201 Created")
    void testCreateBlackoutWithStaffTokenReturns201() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 28);
        CreateBlackoutRequest request = new CreateBlackoutRequest("ROOM", null, date, "Deep Cleaning");
        AdminBlackoutDto dto = new AdminBlackoutDto(2L, 1L, "ROOM", "Estate Room", date, "Deep Cleaning");

        when(service.createBlackout(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/admin/blackouts")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.roomCategoryCode").value("ROOM"))
                .andExpect(jsonPath("$.blackoutDate").value("2026-11-28"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/blackouts returns 409 Conflict when duplicate blackout date is requested")
    void testCreateBlackoutDuplicateReturns409() throws Exception {
        LocalDate date = LocalDate.of(2026, 12, 24);
        CreateBlackoutRequest request = new CreateBlackoutRequest(null, null, date, "Christmas Buyout");

        when(service.createBlackout(any()))
                .thenThrow(new BlackoutDateConflictException("A resort-wide blackout date already exists on " + date));

        mockMvc.perform(post("/api/v1/admin/blackouts")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Blackout Date Conflict"));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/blackouts/{id} returns 204 No Content")
    void testDeleteBlackoutReturns204() throws Exception {
        doNothing().when(service).deleteBlackout(5L);

        mockMvc.perform(delete("/api/v1/admin/blackouts/5")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isNoContent());

        verify(service, times(1)).deleteBlackout(5L);
    }
}
