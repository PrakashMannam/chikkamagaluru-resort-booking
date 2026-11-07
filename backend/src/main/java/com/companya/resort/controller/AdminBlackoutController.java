package com.companya.resort.controller;

import com.companya.resort.dto.AdminBlackoutDto;
import com.companya.resort.dto.CreateBlackoutRequest;
import com.companya.resort.service.AdminBlackoutPromoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/blackouts")
public class AdminBlackoutController {

    private final AdminBlackoutPromoService service;

    public AdminBlackoutController(AdminBlackoutPromoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<AdminBlackoutDto>> getBlackouts(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(service.getBlackouts(from, to));
    }

    @PostMapping
    public ResponseEntity<AdminBlackoutDto> createBlackout(@Valid @RequestBody CreateBlackoutRequest request) {
        AdminBlackoutDto created = service.createBlackout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlackout(@PathVariable("id") Long id) {
        service.deleteBlackout(id);
        return ResponseEntity.noContent().build();
    }
}
