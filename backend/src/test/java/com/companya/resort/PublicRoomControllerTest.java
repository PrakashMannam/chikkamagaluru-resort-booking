package com.companya.resort;

import com.companya.resort.config.SecurityConfig;
import com.companya.resort.controller.PublicRoomController;
import com.companya.resort.dto.*;
import com.companya.resort.exception.BlackoutDateConflictException;
import com.companya.resort.exception.InsufficientAllotmentException;
import com.companya.resort.exception.MaxActiveHoldsExceededException;
import com.companya.resort.security.JwtAccessDeniedHandler;
import com.companya.resort.security.JwtAuthenticationEntryPoint;
import com.companya.resort.security.JwtAuthenticationFilter;
import com.companya.resort.security.JwtService;
import com.companya.resort.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
class PublicRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @Test
    void getAvailability_success_returns200() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(7);

        RoomCategoryAvailabilityDto catDto = new RoomCategoryAvailabilityDto();
        catDto.setId(1L);
        catDto.setCode("ROOM");
        catDto.setName("Coffee Estate Canopy Room");
        catDto.setIsBlackedOut(false);
        catDto.setHasAllotment(true);
        catDto.setRemainingAllotment(8);
        catDto.setStartingRatePerNight(new BigDecimal("8500.00"));

        AvailabilityResponse response = new AvailabilityResponse(checkIn, checkOut, 2, 1, 2, 0, List.of(catDto));

        when(bookingService.checkAvailability(eq(checkIn), eq(checkOut), anyInt(), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/public/rooms/availability")
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString())
                        .param("rooms", "1")
                        .param("adults", "2")
                        .param("children", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nights").value(2))
                .andExpect(jsonPath("$.categories[0].code").value("ROOM"))
                .andExpect(jsonPath("$.categories[0].remainingAllotment").value(8));
    }

    @Test
    void getAvailability_invalidDates_returns400() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(3); // checkout before checkin

        when(bookingService.checkAvailability(eq(checkIn), eq(checkOut), anyInt(), anyInt(), anyInt()))
                .thenThrow(new com.companya.resort.exception.InvalidDateRangeException("Check-out date must be strictly after check-in date"));

        mockMvc.perform(get("/api/v1/public/rooms/availability")
                        .param("checkIn", checkIn.toString())
                        .param("checkOut", checkOut.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Date Range"));
    }

    @Test
    void createBooking_success_returns200() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(7);

        CreateBookingRequest request = new CreateBookingRequest(
                "idem-test-1", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Vikram Rao", "vikram@example.com", "9876543210", null, true
        );

        BookingResponseDto responseDto = new BookingResponseDto();
        responseDto.setBookingRef("X7K9M4P2Q8W5");
        responseDto.setIdempotencyKey("idem-test-1");
        responseDto.setStatus("PENDING_PAYMENT");
        responseDto.setTotalPayableInr(new BigDecimal("17000.00"));
        responseDto.setOneTimePaymentToken("token1234567890123456789012345678901234567890123456789012345678901234");

        when(bookingService.createBookingAndLockAllotment(any(CreateBookingRequest.class), any()))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/public/bookings")
                        .header("Idempotency-Key", "idem-test-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingRef").value("X7K9M4P2Q8W5"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalPayableInr").value(17000.00))
                .andExpect(jsonPath("$.oneTimePaymentToken").isString());
    }

    @Test
    void createBooking_validationFailure_missingFields_returns400() throws Exception {
        // Missing guest email and invalid room count (0)
        CreateBookingRequest invalidRequest = new CreateBookingRequest(
                "idem-invalid", null, null, null, null, 0, 0, 0,
                "", "", "", null, null
        );

        mockMvc.perform(post("/api/v1/public/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void createBooking_validationFailure_invalidPhone_returns400() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(7);
        CreateBookingRequest invalidPhoneRequest = new CreateBookingRequest(
                "idem-phone-val", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Vikram Rao", "vikram@example.com", "0000000000", null, true
        );

        mockMvc.perform(post("/api/v1/public/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPhoneRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details.guestPhone").exists());
    }

    @Test
    void createBooking_blackoutConflict_returns409() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(7);

        CreateBookingRequest request = new CreateBookingRequest(
                "idem-blackout", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Vikram Rao", "vikram@example.com", "9876543210", null, true
        );

        when(bookingService.createBookingAndLockAllotment(any(CreateBookingRequest.class), any()))
                .thenThrow(new BlackoutDateConflictException("Dates conflict with buyout"));

        mockMvc.perform(post("/api/v1/public/bookings")
                        .header("Idempotency-Key", "idem-blackout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Blackout Date Conflict"));
    }

    @Test
    void createBooking_insufficientAllotment_returns409() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(7);

        CreateBookingRequest request = new CreateBookingRequest(
                "idem-sold-out", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Vikram Rao", "vikram@example.com", "9876543210", null, true
        );

        when(bookingService.createBookingAndLockAllotment(any(CreateBookingRequest.class), any()))
                .thenThrow(new InsufficientAllotmentException("Insufficient website allotment"));

        mockMvc.perform(post("/api/v1/public/bookings")
                        .header("Idempotency-Key", "idem-sold-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Insufficient Allotment"));
    }

    @Test
    void createBooking_maxActiveHoldsExceeded_returns429() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(7);

        CreateBookingRequest request = new CreateBookingRequest(
                "idem-greedy", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Vikram Rao", "vikram@example.com", "9876543210", null, true
        );

        when(bookingService.createBookingAndLockAllotment(any(CreateBookingRequest.class), any()))
                .thenThrow(new MaxActiveHoldsExceededException("Guest already has 2 active holds"));

        mockMvc.perform(post("/api/v1/public/bookings")
                        .header("Idempotency-Key", "idem-greedy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Active Hold Cap Exceeded"));
    }

    @Test
    void createBooking_invalidPromoCode_returns422() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(7);

        CreateBookingRequest request = new CreateBookingRequest(
                "idem-promo", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Vikram Rao", "vikram@example.com", "9876543210", "EXPIRED99", true
        );

        when(bookingService.createBookingAndLockAllotment(any(CreateBookingRequest.class), any()))
                .thenThrow(new com.companya.resort.exception.InvalidPromoCodeException("Promo code is expired"));

        mockMvc.perform(post("/api/v1/public/bookings")
                        .header("Idempotency-Key", "idem-promo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Invalid Promo Code"));
    }

    @Test
    void createBooking_idempotencyPayloadMismatch_returns422() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(7);

        CreateBookingRequest request = new CreateBookingRequest(
                "idem-mismatch", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Vikram Rao", "vikram@example.com", "9876543210", null, true
        );

        when(bookingService.createBookingAndLockAllotment(any(CreateBookingRequest.class), any()))
                .thenThrow(new com.companya.resort.exception.IdempotencyPayloadMismatchException("Idempotency key already used with different booking payload"));

        mockMvc.perform(post("/api/v1/public/bookings")
                        .header("Idempotency-Key", "idem-mismatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Idempotency Payload Mismatch"));
    }

    @Test
    void createBooking_lockTimeoutExhausted_returns503() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(7);

        CreateBookingRequest request = new CreateBookingRequest(
                "idem-timeout", 1L, 1L, checkIn, checkOut, 1, 2, 0,
                "Vikram Rao", "vikram@example.com", "9876543210", null, true
        );

        when(bookingService.createBookingAndLockAllotment(any(CreateBookingRequest.class), any()))
                .thenThrow(new com.companya.resort.exception.ServiceUnavailableException("Database lock acquisition timeout after retries"));

        mockMvc.perform(post("/api/v1/public/bookings")
                        .header("Idempotency-Key", "idem-timeout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service Unavailable"));
    }

    @Test
    void getPricingQuote_validParams_returns200AndItemizedQuote() throws Exception {
        PricingQuoteDto quote = new PricingQuoteDto();
        quote.setRoomCategoryId(1L);
        quote.setRoomCategoryCode("ROOM");
        quote.setRoomCategoryName("Coffee Estate Canopy Room");
        quote.setRatePlanId(1L);
        quote.setRatePlanCode("EP");
        quote.setRatePlanName("European Plan");
        quote.setCheckInDate(LocalDate.of(2026, 11, 19));
        quote.setCheckOutDate(LocalDate.of(2026, 11, 22));
        quote.setNights(3);
        quote.setRoomsCount(1);
        quote.setBaseAccommodationTotalInr(new BigDecimal("29500.00"));
        quote.setRatePlanAddonTotalInr(BigDecimal.ZERO);
        quote.setSubtotalBeforeDiscountInr(new BigDecimal("29500.00"));
        quote.setTaxableAmountInr(new BigDecimal("29500.00"));
        quote.setGstPercentageApplied(new BigDecimal("18.00"));
        quote.setGstAmountInr(new BigDecimal("5310.00"));
        quote.setTotalPayableInr(new BigDecimal("34810.00"));
        quote.setGstVerificationStatus("UNVERIFIED");

        when(bookingService.calculatePricingQuote(
                eq(1L), any(), eq(1L), any(),
                eq(LocalDate.of(2026, 11, 19)), eq(LocalDate.of(2026, 11, 22)),
                eq(1), eq(2), eq(0), any()
        )).thenReturn(quote);

        mockMvc.perform(get("/api/v1/public/pricing/quote")
                        .param("roomCategoryId", "1")
                        .param("ratePlanId", "1")
                        .param("checkIn", "2026-11-19")
                        .param("checkOut", "2026-11-22")
                        .param("rooms", "1")
                        .param("adults", "2")
                        .param("children", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseAccommodationTotalInr").value(29500.00))
                .andExpect(jsonPath("$.subtotalBeforeDiscountInr").value(29500.00))
                .andExpect(jsonPath("$.totalPayableInr").value(34810.00))
                .andExpect(jsonPath("$.gstVerificationStatus").value("UNVERIFIED"));
    }

    @Test
    void getPricingQuote_invalidPromoCode_returns422() throws Exception {
        when(bookingService.calculatePricingQuote(
                any(), any(), any(), any(),
                any(LocalDate.class), any(LocalDate.class),
                anyInt(), anyInt(), anyInt(), eq("INVALIDPROMO")
        )).thenThrow(new com.companya.resort.exception.InvalidPromoCodeException("Promo code 'INVALIDPROMO' is invalid or inactive"));

        mockMvc.perform(get("/api/v1/public/pricing/quote")
                        .param("roomCategoryId", "1")
                        .param("ratePlanId", "1")
                        .param("checkIn", "2026-11-19")
                        .param("checkOut", "2026-11-22")
                        .param("rooms", "1")
                        .param("adults", "2")
                        .param("children", "0")
                        .param("promoCode", "INVALIDPROMO"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Invalid Promo Code"))
                .andExpect(jsonPath("$.message").value("Promo code 'INVALIDPROMO' is invalid or inactive"));
    }

    @Test
    void createBooking_exhaustedPromoCode_returns422() throws Exception {
        when(bookingService.createBookingAndLockAllotment(any(), eq("idem-maxed-promo")))
                .thenThrow(new com.companya.resort.exception.InvalidPromoCodeException("Promo code 'MAXED' has reached its maximum uses"));

        String requestJson = """
                {
                    "roomCategoryId": 1,
                    "ratePlanId": 1,
                    "checkInDate": "2026-11-19",
                    "checkOutDate": "2026-11-22",
                    "roomsCount": 1,
                    "adults": 2,
                    "children": 0,
                    "guestName": "Promo Guest",
                    "guestEmail": "guest@example.com",
                    "guestPhone": "9876543210",
                    "promoCode": "MAXED",
                    "dpdpConsentGiven": true
                }
                """;

        mockMvc.perform(post("/api/v1/public/bookings")
                        .header("Idempotency-Key", "idem-maxed-promo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Invalid Promo Code"))
                .andExpect(jsonPath("$.message").value("Promo code 'MAXED' has reached its maximum uses"));
    }
}
