package com.companya.resort;

import com.companya.resort.config.SecurityConfig;
import com.companya.resort.controller.AdminController;
import com.companya.resort.controller.AuthController;
import com.companya.resort.dto.LoginRequest;
import com.companya.resort.entity.Role;
import com.companya.resort.entity.SpecialtyLane;
import com.companya.resort.entity.User;
import com.companya.resort.repository.UserRepository;
import com.companya.resort.security.JwtAccessDeniedHandler;
import com.companya.resort.security.JwtAuthenticationEntryPoint;
import com.companya.resort.security.JwtAuthenticationFilter;
import com.companya.resort.security.JwtService;
import com.companya.resort.seeder.DemoDataSeeder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, AdminController.class})
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
class AuthAndSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(
                "gm.demo@companya-estate.test",
                passwordEncoder.encode("DEMO-ONLY-NOT-REAL-PASS-2026!"),
                "Vikram Rao (GM)",
                Role.STAFF,
                SpecialtyLane.GM_ESCALATION,
                true
        );
        testUser.setId(1L);
    }

    @Test
    void loginSuccessReturnsToken() throws Exception {
        when(userRepository.findByEmail("gm.demo@companya-estate.test")).thenReturn(Optional.of(testUser));

        LoginRequest request = new LoginRequest("gm.demo@companya-estate.test", "DEMO-ONLY-NOT-REAL-PASS-2026!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void wrongPasswordAndUnknownEmailBothReturn401WithSameBody() throws Exception {
        when(userRepository.findByEmail("gm.demo@companya-estate.test")).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("unknown@companya-estate.test")).thenReturn(Optional.empty());

        // 1. Wrong password
        LoginRequest wrongPasswordRequest = new LoginRequest("gm.demo@companya-estate.test", "wrong-password");
        MvcResult wrongPasswordResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // 2. Unknown email
        LoginRequest unknownEmailRequest = new LoginRequest("unknown@companya-estate.test", "DEMO-ONLY-NOT-REAL-PASS-2026!");
        MvcResult unknownEmailResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownEmailRequest)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String body1 = wrongPasswordResult.getResponse().getContentAsString();
        String body2 = unknownEmailResult.getResponse().getContentAsString();

        assertEquals(body1, body2, "Wrong password and unknown email must return the exact same generic response body");
        assertTrue(body1.contains("Invalid email or password"), "Response body must contain 'Invalid email or password'");
    }

    @Test
    void getAdminMeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void getAdminMeWithValidTokenReturnsRightUser() throws Exception {
        String token = jwtService.generateToken(testUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/v1/admin/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Vikram Rao (GM)"))
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.is_gm").value(true))
                .andExpect(jsonPath("$.specialty_lane").value("GM_ESCALATION"));
    }

    @Test
    void authenticatedUserWithoutRoleStaffReceives403Json() throws Exception {
        // Issue token with non-staff role
        String nonStaffToken = jwtService.generateTokenWithRole(testUser, "ROLE_GUEST");

        mockMvc.perform(get("/api/v1/admin/me")
                        .header("Authorization", "Bearer " + nonStaffToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied: insufficient permissions"));
    }

    @Test
    void expiredOrTamperedTokenReturns401() throws Exception {
        String validToken = jwtService.generateToken(testUser);

        // Tampered token
        String tamperedToken = validToken + "tampered";
        mockMvc.perform(get("/api/v1/admin/me")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // Expired token (issued in the past)
        String expiredToken = jwtService.generateTokenWithExpiration(testUser, -60000L);
        mockMvc.perform(get("/api/v1/admin/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void corsPreflightAllowedForConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/admin/me")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")));
    }

    @Test
    void corsPreflightRejectedForDifferentOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/admin/me")
                        .header("Origin", "http://untrusted-site.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isForbidden());
    }

    @Test
    void appRefusesToStartWithShortOrMissingJwtSecret() {
        // Direct bean instantiation checks
        assertThrows(IllegalStateException.class, () -> new JwtService("short-secret"));
        assertThrows(IllegalStateException.class, () -> new JwtService(null));
        assertThrows(IllegalStateException.class, () -> new JwtService("   "));

        // Application context startup failure: secret shorter than 32 characters
        Exception exShort = assertThrows(Exception.class, () -> {
            new SpringApplicationBuilder(ResortApplication.class)
                    .properties(
                            "app.jwt.secret=too-short",
                            "JWT_SECRET=too-short",
                            "spring.datasource.url=jdbc:mysql://localhost:3306/dummy",
                            "spring.flyway.enabled=false"
                    )
                    .run();
        });
        Throwable rootCauseShort = getRootCause(exShort);
        assertTrue(rootCauseShort instanceof IllegalStateException,
                "Root cause must be IllegalStateException, got: " + rootCauseShort.getClass().getName());
        assertEquals("JWT_SECRET must be configured and at least 32 characters long", rootCauseShort.getMessage());

        // Application context startup failure: secret missing / empty
        Exception exMissing = assertThrows(Exception.class, () -> {
            new SpringApplicationBuilder(ResortApplication.class)
                    .properties(
                            "app.jwt.secret=",
                            "JWT_SECRET=",
                            "spring.datasource.url=jdbc:mysql://localhost:3306/dummy",
                            "spring.flyway.enabled=false"
                    )
                    .run();
        });
        Throwable rootCauseMissing = getRootCause(exMissing);
        assertTrue(rootCauseMissing instanceof IllegalStateException,
                "Root cause must be IllegalStateException, got: " + rootCauseMissing.getClass().getName());
        assertEquals("JWT_SECRET must be configured and at least 32 characters long", rootCauseMissing.getMessage());
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    @Test
    void invalidLoginRequestReturns400() throws Exception {
        // Blank email
        LoginRequest blankEmailRequest = new LoginRequest("", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankEmailRequest)))
                .andExpect(status().isBadRequest());

        // Invalid email format
        LoginRequest invalidEmailRequest = new LoginRequest("not-an-email", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                .andExpect(status().isBadRequest());

        // Blank password
        LoginRequest blankPasswordRequest = new LoginRequest("valid@example.com", "   ");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankPasswordRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatedJwtContainsHs256HeaderEvenWithLongSecret() {
        // 1. Standard 32-byte secret
        String token32 = jwtService.generateToken(testUser);
        Jws<Claims> parsed32 = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor("12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token32);
        assertEquals("HS256", parsed32.getHeader().getAlgorithm());

        // 2. Secret longer than 32 bytes (64 bytes - 512 bits)
        String longSecret = "1234567890123456789012345678901234567890123456789012345678901234";
        JwtService longSecretService = new JwtService(longSecret);
        String token64 = longSecretService.generateToken(testUser);

        Jws<Claims> parsed64 = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(longSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token64);
        assertEquals("HS256", parsed64.getHeader().getAlgorithm());
    }

    @Test
    void demoDataSeederCreatesFourUsersIdempotently() {
        List<User> savedUsers = new ArrayList<>();
        when(userRepository.existsByEmail(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            return savedUsers.stream().anyMatch(u -> u.getEmail().equals(email));
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            savedUsers.add(u);
            return u;
        });

        DemoDataSeeder seeder = new DemoDataSeeder(userRepository, passwordEncoder);

        // First run
        seeder.run();
        assertEquals(4, savedUsers.size(), "DemoDataSeeder must create exactly 4 staff users");
        assertTrue(passwordEncoder.matches(DemoDataSeeder.DEMO_PASSWORD, savedUsers.get(0).getPasswordHash()),
                "Passwords must be hashed with BCrypt PasswordEncoder");

        // Second run (idempotency check)
        seeder.run();
        assertEquals(4, savedUsers.size(), "Running DemoDataSeeder twice must not duplicate users");
    }
}
