package com.companya.resort;

import com.companya.resort.entity.*;
import com.companya.resort.repository.BookingRepository;
import com.companya.resort.repository.PaymentRepository;
import com.companya.resort.service.AdminBookingExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminBookingExportServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private AdminBookingExportService exportService;

    private RoomCategory roomCategory;
    private RatePlan ratePlan;

    @BeforeEach
    void setUp() {
        roomCategory = new RoomCategory();
        roomCategory.setCode("HERITAGE_SUITE");
        roomCategory.setName("Heritage Planters Suite");

        ratePlan = new RatePlan();
        ratePlan.setCode("MAP");
        ratePlan.setName("Modified American Plan (Breakfast & Dinner)");
    }

    private Booking createBooking(Long id, String ref, BookingStatus status, LocalDate checkIn) {
        return new Booking(
                id,
                ref,
                "idemp-" + id,
                roomCategory,
                ratePlan,
                null,
                checkIn,
                checkIn.plusDays(2),
                2,
                1,
                2,
                0,
                "Rohit Sharma",
                "rohit.private@example.com",
                "+919876543210",
                true,
                LocalDateTime.now(),
                new BigDecimal("24000.00"),
                BigDecimal.ZERO,
                new BigDecimal("24000.00"),
                new BigDecimal("18.00"),
                new BigDecimal("4320.00"),
                new BigDecimal("28320.00"),
                status,
                LocalDateTime.now().plusHours(1)
        );
    }

    private Payment createPayment(Long id, Booking booking, PaymentStatus status) {
        return new Payment(
                id,
                booking,
                "token-" + id,
                true,
                LocalDateTime.now(),
                "mock_gateway_ref",
                new BigDecimal("28320.00"),
                "UPI",
                status,
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Empty booking list returns header row only")
    void testEmptyBookingListReturnsHeaderOnly() {
        LocalDate date = LocalDate.of(2026, 11, 14);
        when(bookingRepository.findByCheckInDateWithDetails(date)).thenReturn(Collections.emptyList());

        String csv = exportService.exportArrivalsCsv(date);

        String[] lines = csv.split("\r\n");
        assertThat(lines).hasSize(1);
        assertThat(lines[0]).isEqualTo(
                "Booking Reference,Booking Status,Guest Name,Guest Phone,Room Category Code,Room Category Name,Rate Plan Code,Rate Plan Name,Rooms Count,Adults,Children,Check-In Date,Check-Out Date,Nights,Total Amount (INR),GST Amount (INR),Payment Status"
        );
    }

    @Test
    @DisplayName("Standard booking exports with all 17 columns and without sensitive tokens or email")
    void testStandardBookingExport() {
        LocalDate checkIn = LocalDate.of(2026, 11, 14);
        Booking booking = createBooking(100L, "BK-20261114-01", BookingStatus.CONFIRMED, checkIn);
        Payment payment = createPayment(200L, booking, PaymentStatus.SUCCESS);

        when(bookingRepository.findByCheckInDateWithDetails(checkIn)).thenReturn(List.of(booking));
        when(paymentRepository.findByBookingIdIn(List.of(100L))).thenReturn(List.of(payment));

        String csv = exportService.exportArrivalsCsv(checkIn);

        // Security check: Must NOT contain sensitive data
        assertThat(csv).doesNotContain("idemp-100");
        assertThat(csv).doesNotContain("token-200");
        assertThat(csv).doesNotContain("rohit.private@example.com");

        // Verify CSV structure
        String[] lines = csv.split("\r\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[0]).isEqualTo(
                "Booking Reference,Booking Status,Guest Name,Guest Phone,Room Category Code,Room Category Name,Rate Plan Code,Rate Plan Name,Rooms Count,Adults,Children,Check-In Date,Check-Out Date,Nights,Total Amount (INR),GST Amount (INR),Payment Status"
        );
        assertThat(lines[1]).isEqualTo(
                "BK-20261114-01,CONFIRMED,Rohit Sharma,'+919876543210,HERITAGE_SUITE,Heritage Planters Suite,MAP,Modified American Plan (Breakfast & Dinner),1,2,0,2026-11-14,2026-11-16,2,28320.00,4320.00,SUCCESS"
        );
    }

    @Test
    @DisplayName("CONFIRMED booking with SUCCESS payment exports SUCCESS")
    void testConfirmedWithSuccessPaymentExportsSuccess() {
        LocalDate checkIn = LocalDate.of(2026, 11, 14);
        Booking booking = createBooking(101L, "BK-CONF-01", BookingStatus.CONFIRMED, checkIn);
        Payment payment = createPayment(201L, booking, PaymentStatus.SUCCESS);

        when(bookingRepository.findByCheckInDateWithDetails(checkIn)).thenReturn(List.of(booking));
        when(paymentRepository.findByBookingIdIn(List.of(101L))).thenReturn(List.of(payment));

        String csv = exportService.exportArrivalsCsv(checkIn);
        String[] lines = csv.split("\r\n");
        assertThat(lines[1]).endsWith(",SUCCESS");
    }

    @Test
    @DisplayName("PENDING_PAYMENT booking with INITIATED payment exports INITIATED")
    void testPendingPaymentWithInitiatedPaymentExportsInitiated() {
        LocalDate checkIn = LocalDate.of(2026, 11, 14);
        Booking booking = createBooking(102L, "BK-PEND-01", BookingStatus.PENDING_PAYMENT, checkIn);
        Payment payment = createPayment(202L, booking, PaymentStatus.INITIATED);

        when(bookingRepository.findByCheckInDateWithDetails(checkIn)).thenReturn(List.of(booking));
        when(paymentRepository.findByBookingIdIn(List.of(102L))).thenReturn(List.of(payment));

        String csv = exportService.exportArrivalsCsv(checkIn);
        String[] lines = csv.split("\r\n");
        assertThat(lines[1]).endsWith(",INITIATED");
    }

    @Test
    @DisplayName("FAILED payment exports FAILURE regardless of booking status")
    void testFailedPaymentExportsFailure() {
        LocalDate checkIn = LocalDate.of(2026, 11, 14);
        Booking booking = createBooking(103L, "BK-FAIL-01", BookingStatus.FAILED, checkIn);
        Payment payment = createPayment(203L, booking, PaymentStatus.FAILURE);

        when(bookingRepository.findByCheckInDateWithDetails(checkIn)).thenReturn(List.of(booking));
        when(paymentRepository.findByBookingIdIn(List.of(103L))).thenReturn(List.of(payment));

        String csv = exportService.exportArrivalsCsv(checkIn);
        String[] lines = csv.split("\r\n");
        assertThat(lines[1]).endsWith(",FAILURE");
    }

    @Test
    @DisplayName("CONFIRMED booking without Payment exports NOT_RECORDED (no inference from booking status)")
    void testConfirmedWithoutPaymentExportsNotRecorded() {
        LocalDate checkIn = LocalDate.of(2026, 11, 14);
        Booking booking = createBooking(104L, "BK-CONF-NOPAY", BookingStatus.CONFIRMED, checkIn);

        when(bookingRepository.findByCheckInDateWithDetails(checkIn)).thenReturn(List.of(booking));
        when(paymentRepository.findByBookingIdIn(List.of(104L))).thenReturn(Collections.emptyList());

        String csv = exportService.exportArrivalsCsv(checkIn);
        String[] lines = csv.split("\r\n");
        assertThat(lines[1]).endsWith(",NOT_RECORDED");
    }

    @Test
    @DisplayName("CANCELLED booking without Payment exports NOT_RECORDED")
    void testCancelledWithoutPaymentExportsNotRecorded() {
        LocalDate checkIn = LocalDate.of(2026, 11, 14);
        Booking booking = createBooking(105L, "BK-CANC-NOPAY", BookingStatus.CANCELLED, checkIn);

        when(bookingRepository.findByCheckInDateWithDetails(checkIn)).thenReturn(List.of(booking));
        when(paymentRepository.findByBookingIdIn(List.of(105L))).thenReturn(Collections.emptyList());

        String csv = exportService.exportArrivalsCsv(checkIn);
        String[] lines = csv.split("\r\n");
        assertThat(lines[1]).endsWith(",NOT_RECORDED");
    }

    @Test
    @DisplayName("Payment with null status exports NOT_RECORDED")
    void testPaymentWithNullStatusExportsNotRecorded() {
        LocalDate checkIn = LocalDate.of(2026, 11, 14);
        Booking booking = createBooking(106L, "BK-NULL-PAY", BookingStatus.CONFIRMED, checkIn);
        Payment payment = createPayment(206L, booking, null);

        when(bookingRepository.findByCheckInDateWithDetails(checkIn)).thenReturn(List.of(booking));
        when(paymentRepository.findByBookingIdIn(List.of(106L))).thenReturn(List.of(payment));

        String csv = exportService.exportArrivalsCsv(checkIn);
        String[] lines = csv.split("\r\n");
        assertThat(lines[1]).endsWith(",NOT_RECORDED");
    }

    @Test
    @DisplayName("Formula injection attempts in guest fields are neutralized with single quote prefix")
    void testFormulaInjectionNeutralized() {
        LocalDate checkIn = LocalDate.of(2026, 11, 14);

        Booking injectionBooking = new Booking(
                107L,
                "BK-INJ-001",
                "idemp-key",
                roomCategory,
                ratePlan,
                null,
                checkIn,
                checkIn.plusDays(1),
                1,
                1,
                1,
                0,
                "=cmd|' /C calc'!A0",
                "attacker@example.com",
                "+91=SUM(1,2)",
                true,
                LocalDateTime.now(),
                new BigDecimal("10000.00"),
                BigDecimal.ZERO,
                new BigDecimal("10000.00"),
                new BigDecimal("18.00"),
                new BigDecimal("1800.00"),
                new BigDecimal("11800.00"),
                BookingStatus.PENDING_PAYMENT,
                LocalDateTime.now().plusHours(1)
        );

        when(bookingRepository.findByCheckInDateWithDetails(checkIn)).thenReturn(List.of(injectionBooking));
        when(paymentRepository.findByBookingIdIn(List.of(107L))).thenReturn(Collections.emptyList());

        String csv = exportService.exportArrivalsCsv(checkIn);

        String[] lines = csv.split("\r\n");
        assertThat(lines).hasSize(2);
        // Guest name should be prefixed with ' to neutralize formula injection
        assertThat(lines[1]).contains("'=cmd|' /C calc'!A0");
        // Payment status when no payment record exists should be NOT_RECORDED
        assertThat(lines[1]).endsWith(",NOT_RECORDED");
    }
}
