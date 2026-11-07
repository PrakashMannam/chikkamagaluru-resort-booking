package com.companya.resort.controller;

import com.companya.resort.dto.AdminPromoDto;
import com.companya.resort.dto.CreatePromoRequest;
import com.companya.resort.dto.UpdatePromoActiveRequest;
import com.companya.resort.service.AdminBlackoutPromoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/promos")
public class AdminPromoController {

    private final AdminBlackoutPromoService service;

    public AdminPromoController(AdminBlackoutPromoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<AdminPromoDto>> getAllPromos() {
        return ResponseEntity.ok(service.getAllPromos());
    }

    @PostMapping
    public ResponseEntity<AdminPromoDto> createPromo(@Valid @RequestBody CreatePromoRequest request) {
        AdminPromoDto created = service.createPromo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<AdminPromoDto> updatePromoActive(
            @PathVariable("id") Long id,
            @RequestBody UpdatePromoActiveRequest request
    ) {
        AdminPromoDto updated = service.updatePromoActive(id, request);
        return ResponseEntity.ok(updated);
    }
}
