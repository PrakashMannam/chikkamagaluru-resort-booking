package com.companya.resort.service;

import com.companya.resort.entity.Booking;
import com.companya.resort.entity.BookingStatus;
import com.companya.resort.entity.Payment;
import com.companya.resort.repository.BookingRepository;
import com.companya.resort.repository.PaymentRepository;
import com.companya.resort.util.CsvExporter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class AdminBookingExportService {

    public static final List<String> CSV_HEADERS = List.of(
            "Booking Reference",
            "Booking Status",
            "Guest Name",
            "Guest Phone",
            "Room Category Code",
            "Room Category Name",
            "Rate Plan Code",
            "Rate Plan Name",
            "Rooms Count",
            "Adults",
            "Children",
            "Check-In Date",
            "Check-Out Date",
            "Nights",
            "Total Amount (INR)",
            "GST Amount (INR)",
            "Payment Status"
    );

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public AdminBookingExportService(BookingRepository bookingRepository, PaymentRepository paymentRepository) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public String exportArrivalsCsv(LocalDate checkInDate) {
        List<Booking> bookings = bookingRepository.findByCheckInDateWithDetails(checkInDate);

        Map<Long, Payment> paymentMap = new HashMap<>();
        if (!bookings.isEmpty()) {
            List<Long> bookingIds = bookings.stream().map(Booking::getId).toList();
            List<Payment> payments = paymentRepository.findByBookingIdIn(bookingIds);
            for (Payment p : payments) {
                if (p.getBooking() != null && p.getBooking().getId() != null) {
                    paymentMap.put(p.getBooking().getId(), p);
                }
            }
        }

        List<List<String>> rows = new ArrayList<>(bookings.size());
        for (Booking b : bookings) {
            Payment payment = paymentMap.get(b.getId());
            String paymentStatus = (payment != null && payment.getStatus() != null)
                    ? payment.getStatus().name()
                    : "NOT_RECORDED";

            List<String> row = List.of(
                    b.getBookingRef() != null ? b.getBookingRef() : "",
                    b.getStatus() != null ? b.getStatus().name() : "",
                    b.getGuestName() != null ? b.getGuestName() : "",
                    b.getGuestPhone() != null ? b.getGuestPhone() : "",
                    b.getRoomCategory() != null && b.getRoomCategory().getCode() != null ? b.getRoomCategory().getCode() : "",
                    b.getRoomCategory() != null && b.getRoomCategory().getName() != null ? b.getRoomCategory().getName() : "",
                    b.getRatePlan() != null && b.getRatePlan().getCode() != null ? b.getRatePlan().getCode() : "",
                    b.getRatePlan() != null && b.getRatePlan().getName() != null ? b.getRatePlan().getName() : "",
                    b.getRoomsCount() != null ? String.valueOf(b.getRoomsCount()) : "1",
                    b.getAdults() != null ? String.valueOf(b.getAdults()) : "0",
                    b.getChildren() != null ? String.valueOf(b.getChildren()) : "0",
                    b.getCheckInDate() != null ? b.getCheckInDate().toString() : "",
                    b.getCheckOutDate() != null ? b.getCheckOutDate().toString() : "",
                    b.getNights() != null ? String.valueOf(b.getNights()) : "0",
                    b.getTotalPayableInr() != null ? b.getTotalPayableInr().toPlainString() : "0.00",
                    b.getGstAmountInr() != null ? b.getGstAmountInr().toPlainString() : "0.00",
                    paymentStatus
            );
            rows.add(row);
        }

        return CsvExporter.buildCsv(CSV_HEADERS, rows);
    }
}
