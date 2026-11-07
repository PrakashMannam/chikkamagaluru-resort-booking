package com.companya.resort;

import com.companya.resort.config.SecurityConfig;
import com.companya.resort.controller.AdminPromoController;
import com.companya.resort.dto.AdminPromoDto;
import com.companya.resort.dto.CreatePromoRequest;
import com.companya.resort.dto.UpdatePromoActiveRequest;
import com.companya.resort.entity.DiscountType;
import com.companya.resort.entity.Role;
import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminPromoController.class})
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
public class AdminPromoEndpointsWebMvcTest {

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
    @DisplayName("GET /api/v1/admin/promos without token returns 401 Unauthorized")
    void testGetPromosWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/promos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/admin/promos with ROLE_STAFF returns 200 OK")
    void testGetPromosWithStaffTokenReturns200() throws Exception {
        AdminPromoDto dto = new AdminPromoDto(
                1L, "DIRECT10", DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                LocalDate.of(2026, 11, 1), LocalDate.of(2027, 4, 30), 2, 1000, 5, true
        );

        when(service.getAllPromos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/admin/promos")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("DIRECT10"))
                .andExpect(jsonPath("$[0].discountType").value("PERCENTAGE"))
                .andExpect(jsonPath("$[0].discountValue").value(10.00))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/admin/promos creates a promo and returns 201 Created")
    void testCreatePromoWithStaffTokenReturns201() throws Exception {
        CreatePromoRequest req = new CreatePromoRequest(
                "COFFEE20", DiscountType.PERCENTAGE, new BigDecimal("20.00"),
                LocalDate.of(2026, 11, 1), LocalDate.of(2027, 4, 30), 2, 500, true
        );
        AdminPromoDto dto = new AdminPromoDto(
                2L, "COFFEE20", DiscountType.PERCENTAGE, new BigDecimal("20.00"),
                LocalDate.of(2026, 11, 1), LocalDate.of(2027, 4, 30), 2, 500, 0, true
        );

        when(service.createPromo(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/admin/promos")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.code").value("COFFEE20"))
                .andExpect(jsonPath("$.discountValue").value(20.00));
    }

    @Test
    @DisplayName("POST /api/v1/admin/promos with duplicate code returns 400 Bad Request")
    void testCreatePromoDuplicateReturns400() throws Exception {
        CreatePromoRequest req = new CreatePromoRequest(
                "DIRECT10", DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                LocalDate.of(2026, 11, 1), LocalDate.of(2027, 4, 30), 2, 500, true
        );

        when(service.createPromo(any()))
                .thenThrow(new IllegalArgumentException("Promo code 'DIRECT10' already exists"));

        mockMvc.perform(post("/api/v1/admin/promos")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Promo code 'DIRECT10' already exists"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/promos/{id}/active toggles active status and returns 200 OK")
    void testUpdatePromoActiveReturns200() throws Exception {
        AdminPromoDto dto = new AdminPromoDto(
                1L, "DIRECT10", DiscountType.PERCENTAGE, new BigDecimal("10.00"),
                LocalDate.of(2026, 11, 1), LocalDate.of(2027, 4, 30), 2, 1000, 5, false
        );

        when(service.updatePromoActive(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(patch("/api/v1/admin/promos/1/active")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePromoActiveRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.isActive").value(false));
    }
}
