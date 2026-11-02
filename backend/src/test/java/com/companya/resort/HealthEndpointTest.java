package com.companya.resort;

import com.companya.resort.config.SecurityConfig;
import com.companya.resort.controller.HealthController;
import com.companya.resort.security.JwtAccessDeniedHandler;
import com.companya.resort.security.JwtAuthenticationEntryPoint;
import com.companya.resort.security.JwtAuthenticationFilter;
import com.companya.resort.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
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
class HealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/health"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("ok"));
    }
}
