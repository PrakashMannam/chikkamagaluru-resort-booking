package com.companya.resort.controller;

import com.companya.resort.dto.AdminDailyInventoryRateDto;
import com.companya.resort.dto.BulkUpdateInventoryRatesRequest;
import com.companya.resort.entity.User;
import com.companya.resort.repository.UserRepository;
import com.companya.resort.security.UserPrincipal;
import com.companya.resort.service.AdminInventoryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final AdminInventoryService adminInventoryService;

    public AdminController(UserRepository userRepository, Optional<AdminInventoryService> adminInventoryService) {
        this.userRepository = userRepository;
        this.adminInventoryService = adminInventoryService.orElse(null);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        Optional<User> userOpt = userRepository.findById(principal.getId());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not found"));
        }

        User user = userOpt.get();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("fullName", user.getFullName());
        response.put("full_name", user.getFullName());
        response.put("role", user.getRole().name());
        response.put("is_gm", user.getIsGm());
        response.put("isGm", user.getIsGm());
        response.put("specialty_lane", user.getSpecialtyLane().name());
        response.put("specialtyLane", user.getSpecialtyLane().name());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/staff")
    public ResponseEntity<?> getStaffMembers() {
        java.util.List<User> users = userRepository.findAll();
        java.util.List<Map<String, Object>> staffList = users.stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("fullName", u.getFullName());
            map.put("full_name", u.getFullName());
            map.put("email", u.getEmail());
            map.put("role", u.getRole().name());
            map.put("isGm", u.getIsGm());
            map.put("is_gm", u.getIsGm());
            map.put("specialtyLane", u.getSpecialtyLane() != null ? u.getSpecialtyLane().name() : null);
            map.put("specialty_lane", u.getSpecialtyLane() != null ? u.getSpecialtyLane().name() : null);
            return map;
        }).toList();

        return ResponseEntity.ok(staffList);
    }

    @GetMapping("/inventory-rates")
    public ResponseEntity<List<AdminDailyInventoryRateDto>> getInventoryRates(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<AdminDailyInventoryRateDto> rates = adminInventoryService.getInventoryRates(from, to);
        return ResponseEntity.ok(rates);
    }

    @PutMapping("/inventory-rates/bulk")
    public ResponseEntity<List<AdminDailyInventoryRateDto>> bulkUpdateInventoryRates(
            @Valid @RequestBody BulkUpdateInventoryRatesRequest request
    ) {
        List<AdminDailyInventoryRateDto> updated = adminInventoryService.bulkUpdateInventoryRates(request);
        return ResponseEntity.ok(updated);
    }
}
