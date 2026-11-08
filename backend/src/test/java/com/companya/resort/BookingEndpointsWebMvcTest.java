package com.companya.resort;

import com.companya.resort.config.SecurityConfig;
import com.companya.resort.controller.PublicRoomController;
import com.companya.resort.dto.BookingResponseDto;
import com.companya.resort.dto.CancelHoldRequest;
import com.companya.resort.dto.PaymentWebhookRequest;
import com.companya.resort.exception.BookingHoldExpiredException;
import com.companya.resort.exception.PaymentTokenAlreadyUsedException;
import com.companya.resort.exception.ResourceNotFoundException;
import com.companya.resort.security.JwtAccessDeniedHandler;
import com.companya.resort.security.JwtAuthenticationEntryPoint;
import com.companya.resort.security.JwtAuthenticationFilter;
import com.companya.resort.security.JwtService;
import com.companya.resort.service.BookingPaymentService;
import com.companya.resort.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicRoomController.class)
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
public class BookingEndpointsWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private BookingPaymentService bookingPaymentService;

    @Test
    void mockPaymentWebhook_success_returns200() throws Exception {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setBookingRef("BKG123456789");
        dto.setStatus("CONFIRMED");
        dto.setGuestEmail("a***@gmail.com");
        dto.setGuestPhone("98XXXXXX21");

        when(bookingPaymentService.processMockWebhook(eq("BKG123456789"), eq("tok123"), any()))
                .thenReturn(dto);

        PaymentWebhookRequest req = new PaymentWebhookRequest("tok123", "SUCCESS");

        mockMvc.perform(post("/api/v1/public/payments/BKG123456789/mock-webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.bookingRef").value("BKG123456789"))
                .andExpect(jsonPath("$.guestEmail").value("a***@gmail.com"))
                .andExpect(jsonPath("$.guestPhone").value("98XXXXXX21"));
    }

    @Test
    void mockPaymentWebhook_expiredHold_returns409() throws Exception {
        when(bookingPaymentService.processMockWebhook(eq("BKG123456789"), eq("tok123"), any()))
                .thenThrow(new BookingHoldExpiredException("Booking hold has expired"));

        PaymentWebhookRequest req = new PaymentWebhookRequest("tok123", "SUCCESS");

        mockMvc.perform(post("/api/v1/public/payments/BKG123456789/mock-webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Booking Hold Expired"));
    }

    @Test
    void mockPaymentWebhook_reusedToken_returns409() throws Exception {
        when(bookingPaymentService.processMockWebhook(eq("BKG123456789"), eq("tok123"), any()))
                .thenThrow(new PaymentTokenAlreadyUsedException("Payment token has already been used"));

        PaymentWebhookRequest req = new PaymentWebhookRequest("tok123", "SUCCESS");

        mockMvc.perform(post("/api/v1/public/payments/BKG123456789/mock-webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Payment Token Already Used"));
    }

    @Test
    void cancelHold_success_returns200() throws Exception {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setBookingRef("BKG123456789");
        dto.setStatus("CANCELLED");
        dto.setGuestEmail("a***@gmail.com");
        dto.setGuestPhone("98XXXXXX21");

        when(bookingPaymentService.cancelHold(eq("BKG123456789"), eq("tok123")))
                .thenReturn(dto);

        CancelHoldRequest req = new CancelHoldRequest("tok123");

        mockMvc.perform(post("/api/v1/public/bookings/BKG123456789/cancel-hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.guestEmail").value("a***@gmail.com"))
                .andExpect(jsonPath("$.guestPhone").value("98XXXXXX21"));
    }

    @Test
    void getBooking_success_returnsMaskedFields() throws Exception {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setBookingRef("BKG123456789");
        dto.setGuestEmail("a***@gmail.com");
        dto.setGuestPhone("98XXXXXX21");
        // Ensure token is null as returned by getBookingMasked
        dto.setOneTimePaymentToken(null);

        when(bookingPaymentService.getBookingMasked(eq("BKG123456789"))).thenReturn(dto);

        mockMvc.perform(get("/api/v1/public/bookings/BKG123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingRef").value("BKG123456789"))
                .andExpect(jsonPath("$.guestEmail").value("a***@gmail.com"))
                .andExpect(jsonPath("$.guestPhone").value("98XXXXXX21"))
                .andExpect(jsonPath("$.oneTimePaymentToken").doesNotExist());
    }

    @Test
    void getBooking_doesNotExposeSensitiveTokens_evenIfPending() throws Exception {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setBookingRef("BKG987654321");
        dto.setStatus("PENDING_PAYMENT");
        dto.setGuestEmail("j***@example.com");
        dto.setGuestPhone("91XXXXXX00");
        dto.setOneTimePaymentToken(null);

        when(bookingPaymentService.getBookingMasked(eq("BKG987654321"))).thenReturn(dto);

        mockMvc.perform(get("/api/v1/public/bookings/BKG987654321"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.oneTimePaymentToken").doesNotExist());
    }

    @Test
    void unauthorizedCaller_cannotCancelHold_withoutToken() throws Exception {
        when(bookingPaymentService.cancelHold(eq("BKG123456789"), eq(null)))
                .thenThrow(new IllegalArgumentException("One-time payment token is required to cancel hold"));

        mockMvc.perform(post("/api/v1/public/bookings/BKG123456789/cancel-hold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("One-time payment token is required to cancel hold"));
    }

    @Test
    void cancelHold_tokenInQueryString_isNotAccepted() throws Exception {
        when(bookingPaymentService.cancelHold(eq("BKG123456789"), eq(null)))
                .thenThrow(new IllegalArgumentException("One-time payment token is required to cancel hold"));

        mockMvc.perform(post("/api/v1/public/bookings/BKG123456789/cancel-hold")
                        .param("token", "synthetic-token-not-for-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(bookingPaymentService).cancelHold(eq("BKG123456789"), eq(null));
    }

    @Test
    void unauthorizedCaller_cannotTriggerPayment_withoutToken() throws Exception {
        when(bookingPaymentService.processMockWebhook(eq("BKG123456789"), eq(null), any()))
                .thenThrow(new IllegalArgumentException("One-time payment token is required"));

        mockMvc.perform(post("/api/v1/public/payments/BKG123456789/mock-webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUCCESS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("One-time payment token is required"));
    }

    @Test
    void legitimateGuest_withToken_canCancelHold() throws Exception {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setBookingRef("BKG123456789");
        dto.setStatus("CANCELLED");
        dto.setGuestEmail("a***@gmail.com");
        dto.setGuestPhone("98XXXXXX21");
        dto.setOneTimePaymentToken(null);

        when(bookingPaymentService.cancelHold(eq("BKG123456789"), eq("valid-secure-token-12345")))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/public/bookings/BKG123456789/cancel-hold")
                        .header("X-Payment-Token", "valid-secure-token-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.oneTimePaymentToken").doesNotExist());
    }

    @Test
    void getBooking_unknownRef_returns404() throws Exception {
        when(bookingPaymentService.getBookingMasked(eq("UNKNOWN12345")))
                .thenThrow(new ResourceNotFoundException("Booking not found"));

        mockMvc.perform(get("/api/v1/public/bookings/UNKNOWN12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }
    @Test
    void bookingResponseDto_expiresAt_serializesAsUtcIsoInstantEndingWithZ() throws Exception {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setBookingRef("BKG123456789");
        dto.setStatus("PENDING_PAYMENT");
        dto.setGuestEmail("a***@gmail.com");
        dto.setGuestPhone("98XXXXXX21");
        Instant instant = Instant.parse("2026-11-08T04:15:30Z");
        dto.setExpiresAt(instant);

        when(bookingPaymentService.getBookingMasked(eq("BKG123456789"))).thenReturn(dto);

        mockMvc.perform(get("/api/v1/public/bookings/BKG123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresAt").value("2026-11-08T04:15:30Z"));
    }
}
