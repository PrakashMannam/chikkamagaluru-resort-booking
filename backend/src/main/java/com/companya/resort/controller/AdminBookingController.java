package com.companya.resort.controller;

import com.companya.resort.service.AdminBookingExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/bookings")
public class AdminBookingController {

    private final AdminBookingExportService exportService;

    public AdminBookingController(AdminBookingExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportDailyBookings(
            @RequestParam(value = "date", required = false) String dateStr
    ) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Date parameter is required in YYYY-MM-DD format"));
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid date format. Expected YYYY-MM-DD"));
        }

        String csv = exportService.exportArrivalsCsv(date);
        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);

        String filename = "daily-bookings-" + date + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csvBytes);
    }
}
