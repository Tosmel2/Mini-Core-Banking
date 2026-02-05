package com.banking.controller;

import com.banking.dto.*;
import com.banking.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Loans", description = "Loan management APIs")
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    @Operation(summary = "Apply for a loan")
    public ResponseEntity<LoanApplicationResponse> applyForLoan(
            @Valid @RequestBody LoanApplicationRequest request) {
        LoanApplicationResponse response = loanService.applyForLoan(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all loans for authenticated user")
    public ResponseEntity<LoanListResponse> getUserLoans() {
        LoanListResponse response = loanService.getUserLoans();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "Get loan details")
    public ResponseEntity<LoanDto> getLoanDetails(@PathVariable Long loanId) {
        LoanDto response = loanService.getLoanDetails(loanId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{loanId}/repay")
    @Operation(summary = "Make a loan repayment")
    public ResponseEntity<LoanRepaymentResponse> makeLoanRepayment(
            @PathVariable Long loanId,
            @Valid @RequestBody LoanRepaymentRequest request) {
        LoanRepaymentResponse response = loanService.makeLoanRepayment(loanId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{loanId}/repayments")
    @Operation(summary = "Get repayment history for a loan")
    public ResponseEntity<LoanRepaymentListResponse> getLoanRepayments(@PathVariable Long loanId) {
        LoanRepaymentListResponse response = loanService.getLoanRepayments(loanId);
        return ResponseEntity.ok(response);
    }
}