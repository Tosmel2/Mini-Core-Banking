package com.banking.controller;

import com.banking.dto.*;
import com.banking.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin management APIs")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "Get all users (Admin only)")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<UserDto> users = adminService.getAllUsers(search, page, size);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{userId}/status")
    @Operation(summary = "Activate or deactivate a user")
    public ResponseEntity<UserDto> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> request) {

        boolean isActive = request.getOrDefault("isActive", true);
        UserDto user = adminService.updateUserStatus(userId, isActive);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/loans/pending")
    @Operation(summary = "Get all pending loan applications")
    public ResponseEntity<Page<LoanDto>> getPendingLoans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<LoanDto> loans = adminService.getPendingLoans(page, size);
        return ResponseEntity.ok(loans);
    }

    @PutMapping("/loans/{loanId}/approve")
    @Operation(summary = "Approve a loan application")
    public ResponseEntity<LoanDto> approveLoan(
            @PathVariable Long loanId,
            @Valid @RequestBody LoanApprovalRequest request) {

        LoanDto loan = adminService.approveLoan(loanId, request);
        return ResponseEntity.ok(loan);
    }

    @PutMapping("/loans/{loanId}/reject")
    @Operation(summary = "Reject a loan application")
    public ResponseEntity<LoanDto> rejectLoan(
            @PathVariable Long loanId,
            @Valid @RequestBody LoanRejectionRequest request) {

        LoanDto loan = adminService.rejectLoan(loanId, request);
        return ResponseEntity.ok(loan);
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = adminService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
}